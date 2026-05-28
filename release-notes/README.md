# Release Notes

## Русский

Актуальные release notes для ветки `0.56.x` остаются в основных файлах changelog:

- Русский: [changelog_ru.md](../changelog_ru.md)
- Английский: [changelog_en.md](../changelog_en.md)

Архив release notes по веткам релизов:

Соглашение по именованию: файлы вида `0.53.x.md` или `0.42.x.md` обозначают архив ветки релизов, а не строгий semver-wildcard.
Суффикс `x` используется как обозначение семейства релизов внутри одной ветки.

| Ветка релизов | Русский | Английский |
| --- | --- | --- |
| `0.55.x` | [ru/0.55.x.md](./ru/0.55.x.md) | [en/0.55.x.md](./en/0.55.x.md) |
| `0.54.x` | [ru/0.54.x.md](./ru/0.54.x.md) | [en/0.54.x.md](./en/0.54.x.md) |
| `0.53.x` | [ru/0.53.x.md](./ru/0.53.x.md) | [en/0.53.x.md](./en/0.53.x.md) |
| `0.52.x` | [ru/0.52.x.md](./ru/0.52.x.md) | [en/0.52.x.md](./en/0.52.x.md) |
| `0.51.x` | [ru/0.51.x.md](./ru/0.51.x.md) | [en/0.51.x.md](./en/0.51.x.md) |
| `0.50.x` | [ru/0.50.x.md](./ru/0.50.x.md) | [en/0.50.x.md](./en/0.50.x.md) |
| `0.49.x` | [ru/0.49.x.md](./ru/0.49.x.md) | [en/0.49.x.md](./en/0.49.x.md) |
| `0.48.x` | [ru/0.48.x.md](./ru/0.48.x.md) | [en/0.48.x.md](./en/0.48.x.md) |
| `0.47.x` | [ru/0.47.x.md](./ru/0.47.x.md) | [en/0.47.x.md](./en/0.47.x.md) |
| `0.46.x` | [ru/0.46.x.md](./ru/0.46.x.md) | [en/0.46.x.md](./en/0.46.x.md) |
| `0.45.x` | [ru/0.45.x.md](./ru/0.45.x.md) | [en/0.45.x.md](./en/0.45.x.md) |
| `0.44.x` | [ru/0.44.x.md](./ru/0.44.x.md) | [en/0.44.x.md](./en/0.44.x.md) |
| `0.43.x` | [ru/0.43.x.md](./ru/0.43.x.md) | [en/0.43.x.md](./en/0.43.x.md) |
| `0.42.x` | [ru/0.42.x.md](./ru/0.42.x.md) | [en/0.42.x.md](./en/0.42.x.md) |

## English

Current `0.56.x` release notes remain in the root changelog files:

- Russian: [changelog_ru.md](../changelog_ru.md)
- English: [changelog_en.md](../changelog_en.md)

Archived release notes by release branch:

Naming convention: files such as `0.53.x.md` or `0.42.x.md` represent a release branch archive, not a strict semantic-version wildcard.
The `x` suffix is used to group all release notes that belong to the same branch family.

| Release branch | Russian | English |
| --- | --- | --- |
| `0.55.x` | [ru/0.55.x.md](./ru/0.55.x.md) | [en/0.55.x.md](./en/0.55.x.md) |
| `0.54.x` | [ru/0.54.x.md](./ru/0.54.x.md) | [en/0.54.x.md](./en/0.54.x.md) |
| `0.53.x` | [ru/0.53.x.md](./ru/0.53.x.md) | [en/0.53.x.md](./en/0.53.x.md) |
| `0.52.x` | [ru/0.52.x.md](./ru/0.52.x.md) | [en/0.52.x.md](./en/0.52.x.md) |
| `0.51.x` | [ru/0.51.x.md](./ru/0.51.x.md) | [en/0.51.x.md](./en/0.51.x.md) |
| `0.50.x` | [ru/0.50.x.md](./ru/0.50.x.md) | [en/0.50.x.md](./en/0.50.x.md) |
| `0.49.x` | [ru/0.49.x.md](./ru/0.49.x.md) | [en/0.49.x.md](./en/0.49.x.md) |
| `0.48.x` | [ru/0.48.x.md](./ru/0.48.x.md) | [en/0.48.x.md](./en/0.48.x.md) |
| `0.47.x` | [ru/0.47.x.md](./ru/0.47.x.md) | [en/0.47.x.md](./en/0.47.x.md) |
| `0.46.x` | [ru/0.46.x.md](./ru/0.46.x.md) | [en/0.46.x.md](./en/0.46.x.md) |
| `0.45.x` | [ru/0.45.x.md](./ru/0.45.x.md) | [en/0.45.x.md](./en/0.45.x.md) |
| `0.44.x` | [ru/0.44.x.md](./ru/0.44.x.md) | [en/0.44.x.md](./en/0.44.x.md) |
| `0.43.x` | [ru/0.43.x.md](./ru/0.43.x.md) | [en/0.43.x.md](./en/0.43.x.md) |
| `0.42.x` | [ru/0.42.x.md](./ru/0.42.x.md) | [en/0.42.x.md](./en/0.42.x.md) |

## Как архивировать следующую ветку релизов

Когда текущая ветка релизов перестаёт быть актуальной, выполните следующие шаги:

1. Создайте файл `release-notes/ru/0.xx.x.md` и перенесите туда все записи этой ветки из `changelog_ru.md`.
2. Создайте файл `release-notes/en/0.xx.x.md` и перенесите туда все записи этой ветки из `changelog_en.md`.
3. Удалите перенесённые записи из основных `changelog_ru.md` и `changelog_en.md`, оставив там только текущую активную ветку релизов.
4. Добавьте новую строку для архивированной ветки в таблицу выше.
5. Обновите вводный текст в этом файле, если изменилась текущая активная ветка релизов.
6. Используйте то же соглашение по именованию: `0.xx.x.md` означает ветку релизов, а не обязательное наличие каждой patch-версии внутри файла.

Пример: когда ветка `0.56.x` станет архивной, её нужно вынести в `release-notes/ru/0.56.x.md` и `release-notes/en/0.56.x.md`, затем добавить `0.56.x` в таблицу архива.

## How to archive the next release branch

When the current release branch is no longer the active one, follow these steps:

1. Create `release-notes/ru/0.xx.x.md` and move all entries for that branch from `changelog_ru.md` into it.
2. Create `release-notes/en/0.xx.x.md` and move all entries for that branch from `changelog_en.md` into it.
3. Remove the moved entries from the main `changelog_ru.md` and `changelog_en.md` files, leaving only the current active release branch there.
4. Add a new row for the archived branch to the table above.
5. Update the introductory text in this file if the current active release branch has changed.
6. Keep the same naming convention: `0.xx.x.md` represents a release branch, not a requirement that every patch version exists in that file.

Example: when `0.56.x` becomes archival, move it to `release-notes/ru/0.56.x.md` and `release-notes/en/0.56.x.md`, then add `0.56.x` to the archive table.
