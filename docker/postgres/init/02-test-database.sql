-- Banco isolado para os testes de integração do backend
CREATE DATABASE testyourluck_test;
\c testyourluck_test
CREATE EXTENSION IF NOT EXISTS pgcrypto;
