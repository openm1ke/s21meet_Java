# Мониторинг

## Что используется

- `Prometheus` — сбор метрик приложений и инфраструктуры.
- `Grafana` — визуализация метрик и дашборды.

Файлы конфигурации:

- Prometheus: `monitoring/prometheus/prometheus.yml`
- Provisioning datasource Grafana: `monitoring/grafana/provisioning/datasources/datasource.yml`

## Дашборды Grafana

Сейчас в репозитории хранится следующий JSON-дашборд:

- `MainDashboard s21meet-1772570000582.json`

Актуальный путь к файлу импорта:

- `monitoring/dashboards/MainDashboard s21meet-1772570000582.json`

## Как импортировать дашборд в Grafana

1. Открой Grafana (`Dashboards` -> `Import`).
2. Нажми `Upload JSON file`.
3. Выбери файл `monitoring/dashboards/MainDashboard s21meet-1772570000582.json`.
4. Выбери datasource (обычно `Prometheus`).
5. Нажми `Import`.

## Важно

- В текущей структуре provisioning настроен только для datasource.
- Импорт dashboard выполняется вручную из JSON-файла, указанного выше.
