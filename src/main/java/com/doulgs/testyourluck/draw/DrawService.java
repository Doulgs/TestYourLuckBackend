package com.doulgs.testyourluck.draw;

import com.doulgs.testyourluck.catalog.Product;
import com.doulgs.testyourluck.catalog.ProductRepository;
import com.doulgs.testyourluck.dashboard.DashboardCache;
import com.doulgs.testyourluck.draw.DrawDtos.DrawPool;
import com.doulgs.testyourluck.draw.DrawDtos.DrawRequest;
import com.doulgs.testyourluck.draw.DrawDtos.DrawResult;
import com.doulgs.testyourluck.draw.DrawDtos.DrawnProduct;
import com.doulgs.testyourluck.draw.DrawDtos.PoolItem;
import com.doulgs.testyourluck.event.Event;
import com.doulgs.testyourluck.event.EventBlock;
import com.doulgs.testyourluck.event.EventRules;
import com.doulgs.testyourluck.event.EventService;
import com.doulgs.testyourluck.media.MediaService;
import com.doulgs.testyourluck.participant.Participant;
import com.doulgs.testyourluck.participant.ParticipantRepository;
import com.doulgs.testyourluck.shared.tenant.TenantContext;
import com.doulgs.testyourluck.shared.util.Numbers;
import com.doulgs.testyourluck.shared.web.BusinessRuleException;
import com.doulgs.testyourluck.shared.web.NotFoundException;
import com.doulgs.testyourluck.webhook.WebhookTriggerEvents.PlayCompleted;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DrawService {

    static final String NO_STOCK = "Sem estoque disponível neste evento.";
    static final String NO_CREDITS = "Sem créditos disponíveis";

    private final SecureRandom random = new SecureRandom();
    private final Clock clock = Clock.systemUTC();
    private final EventService eventService;
    private final ProductRepository productRepository;
    private final ParticipantRepository participantRepository;
    private final DrawRepository drawRepository;
    private final MediaService mediaService;
    private final RedisLock redisLock;
    private final DashboardCache dashboardCache;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate tx;

    @Transactional(readOnly = true)
    public DrawPool pool(UUID eventId) {
        Event event = eventService.find(eventId);
        long draws = drawRepository.countByEventId(eventId);
        EventBlock block = EventRules.block(event, draws, clock.instant()).orElse(null);
        List<Product> eligible = productRepository.findEligible(eventId);
        int total = eligible.stream().mapToInt(Product::getWeight).sum();
        List<PoolItem> items = eligible.stream().map(p -> new PoolItem(p.getId(), p.getName(), p.getShortCode(),
                p.getHue(), mediaService.url(p.getImageMediaId()), p.getWeight(), p.getQuantity(),
                Numbers.pct(p.getWeight(), total))).toList();
        return new DrawPool(eventId, event.getName(), event.getStatus(), draws, block,
                block == null ? null : block.message(), items);
    }

    /** Jogada: consome 1 crédito, sorteia ponderado pelo peso e baixa o estoque, sob lock do evento. */
    public DrawResult draw(DrawRequest req) {
        var t = TenantContext.require();
        Participant participant = participantRepository.findById(req.participantId())
                .orElseThrow(() -> new NotFoundException("Participante não encontrado"));
        DrawResult result = redisLock.withLock("draw-lock:" + t.tenant() + ":" + participant.getEventId(),
                () -> tx.execute(status -> doDraw(participant.getId(), participant.getEventId())));
        dashboardCache.evictCurrentTenant();
        return result;
    }

    private DrawResult doDraw(UUID participantId, UUID eventId) {
        Event event = eventService.find(eventId);
        EventRules.check(event, drawRepository.countByEventId(eventId), clock.instant());
        if (participantRepository.consumeCredit(participantId) == 0) {
            throw new BusinessRuleException(NO_CREDITS);
        }
        List<Product> candidates = new ArrayList<>(productRepository.findEligible(eventId));
        Product winner = null;
        while (!candidates.isEmpty()) {
            Product pick = pick(candidates);
            if (productRepository.decrementIfAvailable(pick.getId()) == 1) {
                winner = pick;
                break;
            }
            candidates.remove(pick);
        }
        if (winner == null) {
            throw new BusinessRuleException(NO_STOCK);
        }
        Participant participant = participantRepository.findById(participantId).orElseThrow();
        Draw draw = new Draw();
        draw.setEventId(eventId);
        draw.setProductId(winner.getId());
        draw.setParticipantId(participantId);
        draw.setEventName(event.getName());
        draw.setProductName(winner.getName());
        draw.setProductShort(winner.getShortCode());
        draw.setProductHue(winner.getHue());
        draw.setDrawnAt(clock.instant());
        drawRepository.save(draw);

        var t = TenantContext.require();
        events.publishEvent(new PlayCompleted(t.tenant(), t.owner(), draw.getId()));
        return new DrawResult(draw.getId(), eventId, event.getName(), participantId, participant.getName(),
                draw.getDrawnAt(), new DrawnProduct(winner.getId(), winner.getName(), winner.getShortCode(),
                winner.getHue(), mediaService.url(winner.getImageMediaId())),
                drawRepository.countByEventId(eventId), productRepository.findEligible(eventId).size(),
                participant.creditsRemaining());
    }

    private Product pick(List<Product> candidates) {
        int total = candidates.stream().mapToInt(Product::getWeight).sum();
        int r = random.nextInt(total);
        for (Product p : candidates) {
            r -= p.getWeight();
            if (r < 0) {
                return p;
            }
        }
        return candidates.getLast();
    }
}
