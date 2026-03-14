# Локальная разработка и тестирование (`dev.sh`)

## Зачем нужен `dev.sh`

`dev.sh` — это единая точка входа для локальной разработки:

- поднимает инфраструктуру (`postgres`, `prometheus`, `grafana`);
- готовит локальные env-файлы из `*.example`;
- собирает нужные Gradle-модули;
- собирает Docker-образы только для выбранных сервисов;
- перезапускает только то, что ты изменил.

Это ускоряет цикл разработки и избавляет от длинных ручных команд.

## Что делает скрипт автоматически

При запуске скрипт:

1. Проверяет, что доступен `docker compose` (или `docker-compose`).
2. Проверяет наличие `docker-compose.yml`.
3. Создает `env/test/*.env` из `env/test/*.env.example`, если файлов нет.
4. Нормализует адреса межсервисного взаимодействия для test-контура:
   - `s21edu -> s21auth/s21bot`
   - `s21bot -> s21edu/s21rocket`
5. Поднимает инфраструктуру и ждёт `postgres` в `healthy`.

## Основные сценарии

### Полный запуск приложения

```bash
./dev.sh
```

По умолчанию это эквивалент запуска всех app-сервисов:
`s21auth s21edu s21bot s21rocket`.

### Запуск только инфраструктуры

```bash
./dev.sh infra
```

### Пересобрать и поднять конкретные сервисы

```bash
./dev.sh s21edu
./dev.sh s21edu s21bot
```

### Быстрый перезапуск контейнеров без сборки

```bash
./dev.sh s21edu --restart
```

### Поднять сервисы без Gradle/Docker сборки

```bash
./dev.sh s21edu --no-build
```

### Выбор прокси для Telegram (`s21bot`)

По умолчанию используется `vless` (`xray-client`).

```bash
./dev.sh s21bot --proxy vless
./dev.sh s21bot --proxy ssh
./dev.sh s21bot --proxy none
```

Для режима `ssh` заполни параметры в `env/test/compose.env`:
- `SSH_TUNNEL_HOST`, `SSH_TUNNEL_PORT`, `SSH_TUNNEL_USER`
- `SSH_TUNNEL_KEY_FILE`, `SSH_TUNNEL_KNOWN_HOSTS_FILE`

### Полный «чистый» цикл (медленнее, но максимально предсказуемо)

```bash
./dev.sh --full
```

Что делает `--full`:

- `compose down`
- полный Gradle build
- сборка всех образов
- `compose up -d` всех сервисов

## Полезные опции

- `--clean` — добавить `clean` перед Gradle-сборкой выбранных модулей.
- `--recreate` — запуск `compose up` с `--force-recreate`.
- `--with-deps` — пересоздавать сервисы вместе с зависимостями (без `--no-deps`).
- `--proxy vless|ssh|none` — выбрать контейнер прокси для `s21bot`.
- `--ps` — показать `docker compose ps` в конце.
- `--down` — остановить стек (`compose down --remove-orphans`).
- `-h|--help` — показать справку.

## Рекомендованный workflow локальной разработки

1. Поднять инфраструктуру один раз:

```bash
./dev.sh infra
```

2. Работать по модульно:

```bash
./dev.sh s21edu
./dev.sh s21bot
```

3. Если менялся контракт между сервисами, использовать:

```bash
./dev.sh s21edu s21bot --recreate --with-deps
```

4. При «грязном» состоянии окружения:

```bash
./dev.sh --full
```

## Что важно знать

- Скрипт использует профиль `infra` и профиль прокси (`proxy-vless`/`proxy-ssh`) в `docker-compose`.
- Если меняешь только Java-код одного модуля, не нужно пересобирать весь проект.
- Файлы `env/test/*.env` локальные и не должны попадать в git.
