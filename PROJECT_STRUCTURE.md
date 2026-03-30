## PROJECT_STRUCTURE.md

# Структура проекта

```
PlayingCards2/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/printer/playingcards2/
│   │   │   │   ├── MainActivity.kt                 # Главное меню
│   │   │   │   ├── SplashActivity.kt               # Экран дисклеймера + проверка обновлений
│   │   │   │   ├── InventoryActivity.kt            # Инвентарь
│   │   │   │   ├── GameActivity.kt                 # Игровой процесс
│   │   │   │   ├── SettingsActivity.kt             # Настройки + промокоды
│   │   │   │   ├── GameModeDialog.kt               # Диалог выбора режима
│   │   │   │   ├── CardDetailDialog.kt             # Диалог деталей карты
│   │   │   │   ├── CardAdapter.kt                  # Адаптер карт в инвентаре
│   │   │   │   ├── GameCardAdapter.kt              # Адаптер карт в игре
│   │   │   │   ├── CreditsAdapter.kt               # Адаптер титров
│   │   │   │   ├── CardSpecialEffect.kt            # Управление особенностями карт (13 карт)
│   │   │   │   ├── UpdateChecker.kt                # Проверка обновлений на GitHub
│   │   │   │   ├── Card.kt                         # Модель карты
│   │   │   │   ├── GameCard.kt                     # Модель карты для игры
│   │   │   │   ├── PromoCode.kt                    # Модель промокода
│   │   │   │   ├── CreditItem.kt                   # Модель титров
│   │   │   │   ├── Rarity.kt                       # Перечисление редкостей
│   │   │   │   └── CardCategory.kt                 # Перечисление категорий
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   │   ├── activity_splash.xml
│   │   │   │   │   ├── activity_main.xml
│   │   │   │   │   ├── activity_inventory.xml
│   │   │   │   │   ├── activity_game.xml
│   │   │   │   │   ├── activity_settings.xml
│   │   │   │   │   ├── item_card.xml
│   │   │   │   │   ├── item_game_card.xml
│   │   │   │   │   ├── item_credit.xml
│   │   │   │   │   ├── dialog_card_detail.xml
│   │   │   │   │   ├── dialog_game_mode.xml
│   │   │   │   │   └── dialog_reviews.xml
│   │   │   │   ├── layout-land/
│   │   │   │   │   ├── activity_splash.xml
│   │   │   │   │   ├── activity_main.xml
│   │   │   │   │   ├── activity_inventory.xml
│   │   │   │   │   ├── activity_game.xml
│   │   │   │   │   ├── activity_settings.xml
│   │   │   │   │   ├── item_card.xml
│   │   │   │   │   ├── item_game_card.xml
│   │   │   │   │   └── dialog_game_mode.xml
│   │   │   │   ├── drawable/
│   │   │   │   │   ├── background_gradient.xml
│   │   │   │   │   ├── bg_settings_gradient.xml
│   │   │   │   │   ├── bg_game_gradient.xml
│   │   │   │   │   ├── bg_blur_square.xml
│   │   │   │   │   ├── bg_blur_square_purple.xml
│   │   │   │   │   ├── bg_status_bar_gradient.xml
│   │   │   │   │   ├── bg_tab_selected.xml
│   │   │   │   │   ├── bg_tab_default.xml
│   │   │   │   │   ├── bg_tab_selected_vertical.xml
│   │   │   │   │   ├── bg_tab_default_vertical.xml
│   │   │   │   │   ├── bg_stats.xml
│   │   │   │   │   ├── bg_special_icons.xml
│   │   │   │   │   ├── bg_special_icon.xml
│   │   │   │   │   ├── bg_review_item.xml
│   │   │   │   │   ├── bg_response.xml
│   │   │   │   │   ├── bg_avatar.xml
│   │   │   │   │   ├── bg_rating_dot.xml
│   │   │   │   │   ├── circle_progress_bg.xml
│   │   │   │   │   ├── circle_sector_green.xml
│   │   │   │   │   ├── circle_sector_red.xml
│   │   │   │   │   ├── circle_sector_orange.xml
│   │   │   │   │   ├── card_back.xml
│   │   │   │   │   ├── gradient_overlay.xml
│   │   │   │   │   ├── button_3d_play.xml
│   │   │   │   │   ├── button_3d_inventory.xml
│   │   │   │   │   ├── button_3d_shop.xml
│   │   │   │   │   ├── button_3d_settings.xml
│   │   │   │   │   ├── button_ripple_effect.xml
│   │   │   │   │   ├── ic_arrow_back.xml
│   │   │   │   │   ├── ic_arrow_forward.xml
│   │   │   │   │   ├── ic_play.xml
│   │   │   │   │   ├── ic_inventory.xml
│   │   │   │   │   ├── ic_shop.xml
│   │   │   │   │   ├── ic_settings.xml
│   │   │   │   │   ├── ic_deck.xml
│   │   │   │   │   ├── ic_category.xml
│   │   │   │   │   ├── ic_unavailable.xml
│   │   │   │   │   ├── ic_lock.xml
│   │   │   │   │   ├── ic_refresh.xml
│   │   │   │   │   ├── ic_sword.xml
│   │   │   │   │   ├── ic_stun.xml
│   │   │   │   │   ├── ic_double_attack.xml
│   │   │   │   │   ├── ic_shield.xml
│   │   │   │   │   ├── ic_heal.xml
│   │   │   │   │   ├── ic_nature.xml
│   │   │   │   │   ├── ic_global_attack.xml
│   │   │   │   │   ├── ic_tea.xml
│   │   │   │   │   ├── ic_skull.xml
│   │   │   │   │   ├── ic_dream.xml
│   │   │   │   │   ├── ic_illusion.xml
│   │   │   │   │   ├── ic_github.xml
│   │   │   │   │   ├── ic_balloon.xml
│   │   │   │   │   ├── ic_offline.xml
│   │   │   │   │   ├── ic_online.xml
│   │   │   │   │   ├── ic_dev.xml
│   │   │   │   │   ├── ic_design.xml
│   │   │   │   │   ├── ic_artist.xml
│   │   │   │   │   ├── ic_music.xml
│   │   │   │   │   ├── ic_test.xml
│   │   │   │   │   ├── ic_thanks.xml
│   │   │   │   │   ├── ic_version.xml
│   │   │   │   │   ├── ic_player_turn.xml
│   │   │   │   │   ├── ic_enemy_turn.xml
│   │   │   │   │   └── student_1.png ... student_25.png
│   │   │   │   ├── values/
│   │   │   │   │   ├── colors.xml
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   ├── styles.xml
│   │   │   │   │   ├── dimens.xml
│   │   │   │   │   └── themes.xml
│   │   │   │   ├── values-night/
│   │   │   │   │   └── themes.xml
│   │   │   │   ├── font/
│   │   │   │   │   ├── playfair_display_regular.ttf
│   │   │   │   │   ├── playfair_display_bold.ttf
│   │   │   │   │   ├── montserrat_regular.ttf
│   │   │   │   │   ├── montserrat_bold.ttf
│   │   │   │   │   └── orbitron_regular.ttf
│   │   │   │   ├── raw/
│   │   │   │   │   └── chudik.json
│   │   │   │   └── animator/
│   │   │   │       └── animator.xml
│   │   │   └── AndroidManifest.xml
│   │   ├── debug/
│   │   └── release/
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── CHANGELOG.md
└── PROJECT_STRUCTURE.md
```
