

embedding model: nomic-embed-text → normalmente retorna vetor 768 dimensões.
Então o VECTOR(768)

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS documents (
  id UUID PRIMARY KEY,
  source TEXT,
  content TEXT NOT NULL,
  embedding VECTOR(768) NOT NULL,
  created_at TIMESTAMP DEFAULT now()
);

-- índice para busca vetorial (cosine)
CREATE INDEX IF NOT EXISTS idx_documents_embedding
ON documents USING hnsw (embedding vector_cosine_ops);
------------------------------------------------------------
modelo leve
docker exec -it ollama ollama pull phi3

o modelo de embeddings no Ollama
docker exec -it ollama ollama pull nomic-embed-text

Instale o modelo de embedding agora:
ollama pull nomic-embed-text

Instale o modelo
ollama pull phi3

Teste manual depois disso:

curl http://localhost:11434/api/embeddings -d '{
  "model": "nomic-embed-text",
  "prompt": "teste"
}'
---------------------------------------------------------------------------


# 🚀 RAG Local com Spring Boot + Ollama + PostgreSQL (pgvector)

Projeto experimental de arquitetura RAG (Retrieval Augmented Generation) rodando 100% local, utilizando:

- Java 17
- Spring Boot 3
- PostgreSQL + pgvector
- Ollama (LLM local)
- WSL2 + Docker Desktop

--- mvn spring-boot:run

# 📌 Status Atual do Ambiente

Infraestrutura validada com sucesso:

- ✔ Docker Desktop funcionando
- ✔ WSL2 configurado com memória adequada
- ✔ PostgreSQL com extensão pgvector ativo
- ✔ Ollama rodando em container
- ✔ Modelo LLM carregado (phi3)
- ✔ API do Ollama respondendo corretamente

---

# 🏗 Arquitetura Atual

Windows
└── WSL2
└── Docker Desktop
├── PostgreSQL + pgvector
└── Ollama (LLM local)

Aplicação Spring Boot conecta:

- PostgreSQL → armazenamento vetorial
- Ollama → geração de respostas e embeddings

---
<img width="1536" height="1024" alt="image" src="https://github.com/user-attachments/assets/52d22ab9-60b7-41b6-a760-f4e7100a65ba" />
https://chatgpt.com/s/m_6995d19aa798819194e459e2814c2cff

# 🐳 Docker

Containers ativos:

- `rag-postgres`
- `ollama`

Verificar containers:

```bash
docker ps

WSL2 Configuração de Memória

Arquivo:

C:\Users\<seu-usuario>\.wslconfig


Conteúdo utilizado:

[wsl2]
memory=10GB
processors=4
swap=4GB


Reinício aplicado com:

wsl --shutdown


Validação:

docker info


Resultado esperado:

Total Memory: ~8GB ou mais

🤖 Ollama

Modelo carregado:

phi3


Download do modelo:

docker exec -it ollama ollama pull phi3

🔎 Teste direto da API Ollama
curl http://localhost:11434/api/generate -d '{
  "model": "phi3",
  "prompt": "Explique o que é RAG em poucas palavras",
  "stream": false
}'


Resposta esperada:

{
  "model": "phi3",
  "response": "...",
  "done": true
}

📡 API Spring Boot

Aplicação iniciada com:

mvn spring-boot:run


Endpoint validado:

POST /rag/ask

