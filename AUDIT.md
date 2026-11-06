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
| Testes | Nenhum | 4 testes + screenshots |
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
- Detecção de malware, duplicados, fotos borradas e APKs antigos: o material web simulava isso; sem implementação real, seria desonesto exibir resultados.
- Ciclos/capacidade de bateria: não disponíveis de forma confiável.
- Perfis de dispositivo fictícios e tema claro/escuro da web: o app usa dados reais do aparelho e tema escuro fixo.

## Limitações

- Fluxo de cache depende do fabricante; o Android pode demorar a atualizar o `StorageStatsManager`.
- Sem permissão de uso, tamanhos ficam “Indisponível” em vez de estimados.
- Exclusão de mídia depende do provedor e das permissões.
- Validação por Robolectric/screenshots; recomenda-se testar em aparelho físico.
