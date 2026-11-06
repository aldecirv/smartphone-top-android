# Auditoria — Smartphone Top Android

Data: 2026-11-06  
Base: `smartphone-top.zip` (web app React/Vite + trechos de Android Compose em `android/`).

## O que o material original contém

- Web app **Smartphone Top** com telas de Início, Limpeza, Arquivos, Bateria, Apps, Segurança, TOP AI, Histórico e Ajustes.
- Dados simulados de arquivos, apps, segurança e limpeza; bateria com leitura real/fallback; armazenamento estimado pelo navegador.
- Trechos Android em **Jetpack Compose** para um limpador de cache:
  - `StorageStatsManager` real;
  - permissão de Acesso a Dados de Uso;
  - fluxo assistido abrindo Configurações do app;
  - `SpaceTracker` com delta real;
  - telas/componentes Compose (`CacheCleanScreen`, `AssistedCleanDialog`, `PermissionExplanationCard`).
- **Não há projeto Android completo**: faltam módulo Gradle raiz, tema, recursos, `MainActivity` completa, Application e demais telas. O `manifest` usa `package=` legado, `QUERY_ALL_PACKAGES`/`MANAGE_EXTERNAL_STORAGE`, e o build usa Compose com versões antigas.
- Vários números da web são fictícios (arquivos, apps, “limpeza profunda”, score de segurança).

## Decisões

| Tema | Material original | Implementação Android |
|---|---|---|
| Plataforma | React + trechos Compose | Kotlin + XML + Material 3 |
| Métricas | Web simulada; bateria parcialmente real | `StatFs`, `ActivityManager`, `BatteryManager` e `StorageStatsManager` reais |
| Cache | Compose parcial | Lista real + fluxo assistido via Configurações oficiais |
| Limpeza direta | Impossível no Android moderno | Não fingida; medição antes/depois, quando o usuário limpa |
| Arquivos | Lista simulada com duplicados/blur/APK | `MediaStore` real (imagens/vídeos/áudio) + exclusão confirmada |
| Apps | Lista simulada | `PackageManager` real, tamanhos e uninstall pelo sistema |
| Segurança | Score/riscos simulados | Auditoria de configurações públicas, sem alegar antivírus |
| TOP AI | Gemini via servidor | Assistente local determinístico, sem rede |
| Histórico | Mock | `SmartStore` com apenas liberações medidas |
| Permissões | `MANAGE_EXTERNAL_STORAGE` etc. | Uso, mídia e escrita antiga; sem acesso total arbitrário |
| Testes | Nenhum | 7 testes + screenshots |
| APK | Não existia | APK assinado, CI e release |

## Escopo implementado

- Início com dados reais, gauge, RAM, bateria, temperatura, segurança e atalhos.
- Limpeza de cache por app com permissão especial e fluxo assistido.
- Apps reais com tamanhos, busca, filtro, abrir, informações e desinstalar.
- Arquivos/mídia reais com filtros e exclusão pelo sistema.
- Bateria real; limitações declaradas.
- Segurança baseada em configurações verificáveis.
- Histórico de espaço realmente liberado.
- Ajustes de permissões e limite de aviso.
- TOP AI local, sem nuvem.
- README, screenshots, CI e release.

## O que foi deliberadamente omitido

- “Limpeza profunda” automática de cache/dados de outros apps: bloqueada pelo Android e exigiria root/acessibilidade, práticas que não foram adotadas.
- Detecção de malware, fotos borradas/semelhantes, arquivos residuais e APKs antigos: o material web simulava isso; sem implementação real, seria desonesto exibir resultados. Duplicadas exatas de mídia foram implementadas com SHA-256; ver revisão 1.1.0.
- Compressão de mídia e perfis de aparelho: recursos do preview sem equivalente nativo seguro nesta versão.
- Ciclos/capacidade de bateria: não disponíveis de forma confiável.
- Perfis de dispositivo fictícios e tema claro/escuro da web: o app usa dados reais do aparelho e tema escuro fixo.

## Limitações

- Fluxo de cache depende do fabricante; o Android pode demorar a atualizar o `StorageStatsManager`.
- Sem permissão de uso, tamanhos ficam “Indisponível” em vez de estimados.
- Exclusão de mídia depende do provedor e das permissões.
- Validação por Robolectric/screenshots; recomenda-se testar em aparelho físico.

## Revisão 1.1.0 — paridade com o preview

O preview em `smartphone-top.ai.studio` foi comparado com a versão 1.0.0. A 1.1.0 amplia a experiência sem copiar dados simulados:

- Navegação de 5 centrais (Início, Limpeza, Arquivos, Bateria, Apps) + folha “Mais” (Segurança, TOP AI, Histórico, Ajustes).
- Aba Bateria com diagnóstico real do `BatteryManager`; ciclos/capacidade continuam explicitamente indisponíveis.
- Início com análise real de cache e mídia em diálogo, além de atalhos para Bateria/TOP AI.
- Arquivos com mídia real, miniaturas, arquivos grandes, capturas antigas, duplicadas exatas por SHA-256, seleção múltipla e exclusão via `MediaStore.createDeleteRequest`.
- Apps com último uso (UsageStatsManager), filtro “sem uso”, versão, instalador, permissões solicitadas e diálogo de detalhes.
- Segurança com apps fora da Play Store e resumo de permissões sensíveis **solicitadas** (não é possível ler permissões concedidas de outros apps).
- Relação de recursos do preview:
  - **Implementado com dados reais**: navegação, cache assistido, mídia, duplicadas exatas, apps, bateria, segurança, histórico, ajustes.
  - **Não implementado / não falsificado**: arquivos residuais, limpeza de APKs, fotos borradas/semelhantes, compressão/otimização de mídia, perfis simulados de aparelho, tema claro/auto, lembretes/notificações e Gemini.
- Motivo: o preview usa dados simulados e recursos web; o Android moderno não fornece acesso irrestrito a arquivos de outros apps, detecção de “foto borrada” confiável ou permissões concedidas de terceiros. Exibir esses resultados sem base real seria enganoso.

## Revisão 1.2.0 — Limpeza Inteligente de Cache

Implementação do módulo profissional solicitado, sobre o app existente (sem recriar o projeto):

- Varredura de todos os pacotes com `StorageStatsManager` e `QUERY_ALL_PACKAGES`.
- Dados por app: nome, ícone, pacote, cache, app+dados ocupados, categoria (`ApplicationInfo.category`), horário da análise e status.
- Lista ordenada por cache, checkboxes, botão individual e ações de seleção.
- Lote assistido: abre `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` por app, aguarda o retorno, mede o delta e continua com confirmação do usuário.
- Relatório com analisados, com cache, operações, não permitidas, estimado antes, liberado medido, data/hora e barras antes/depois.
- Histórico persistente de relatórios de cache.
- Indicador “Cache dos aplicativos” na tela inicial, com última análise e atalho para analisar.
- Testes de persistência e da tela de histórico; screenshots Robolectric.
- **Não implementado**: chamada `ACTION_CLEAR_APP_CACHE` por pacote (não é API pública confiável), root, ADB, acessibilidade e “Limpar dados”. Em aparelho real não disponível neste ambiente (nenhum dispositivo conectado); a validação de campo deve ser feita em HyperOS/Xiaomi e AOSP.
