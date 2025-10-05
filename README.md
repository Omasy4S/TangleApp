# 🧶 TangleApp - Приложение для вязания

> **Бесплатная альтернатива платным приложениям для вязальщиц**  
> Полнофункциональное Android-приложение для управления проектами вязания, счётчиками петель и рядов, схемами и заметками.

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## ✨ Основные возможности

### 🎯 Управление проектами
- ✅ Создание неограниченного количества проектов
- ✅ Добавление нескольких счётчиков в один проект
- ✅ Редактирование названий проектов и счётчиков
- ✅ Быстрое удаление проектов

### 🔢 Счётчики петель и рядов
- ✅ Отдельные счётчики для петель и рядов
- ✅ Увеличение/уменьшение значений кнопками
- ✅ Прямое редактирование значений по клику
- ✅ Автоматическое сохранение прогресса

### 📐 Калькулятор размеров
- ✅ Расчёт необходимого количества петель и рядов
- ✅ На основе текущих параметров образца
- ✅ Пошаговый интерфейс для удобства
- ✅ Точные расчёты для любых размеров

### 📸 Галерея схем
- ✅ Добавление фотографий схем
- ✅ Загрузка PDF-файлов
- ✅ Создание текстовых заметок
- ✅ Просмотр изображений в полноэкранном режиме
- ✅ Открытие PDF через внешние приложения

### 🎨 Настройки оформления
- ✅ Готовые темы (Светлая/Тёмная)
- ✅ Настройка всех цветов интерфейса
- ✅ Выбор иконки для проектов
- ✅ Сохранение персональной темы

### 📲 Виджет на главный экран
- ✅ Быстрый доступ к счётчикам
- ✅ Изменение значений без открытия приложения
- ✅ Три размера виджета (маленький, средний, большой)
- ✅ Добавление нового счётчика прямо из виджета

---

## 🛠️ Технологии

### Основной стек
- **Язык:** Kotlin
- **UI:** WebView + HTML/CSS/JavaScript
- **База данных:** SharedPreferences + LocalStorage
- **Виджеты:** AppWidgetProvider
- **Работа с файлами:** FileProvider, Base64

### Библиотеки
- **Gson** - сериализация данных
- **Pickr** - выбор цветов
- **Material Design** - современный UI

### Архитектура
- Гибридный подход (Native Android + WebView)
- JavaScript Interface для связи Android ↔ WebView
- Оптимизированная работа с SharedPreferences
- Чистая архитектура с разделением на модули

---

## 📂 Структура проекта

```
App/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/app/
│   │   │   │   ├── MainActivity.kt           # Главная активность
│   │   │   │   ├── KnittingCounterWidget.kt  # Виджет счётчика
│   │   │   │   ├── WidgetConfigActivity.kt   # Настройка виджета
│   │   │   │   ├── Models.kt                 # Модели данных
│   │   │   │   └── Utils.kt                  # Утилиты
│   │   │   ├── assets/
│   │   │   │   ├── kiniti.html               # Основной UI
│   │   │   │   ├── kiniti.css                # Стили
│   │   │   │   └── kiniti.js                 # Логика приложения
│   │   │   ├── res/
│   │   │   │   ├── layout/                   # Layout виджетов
│   │   │   │   ├── drawable/                 # Иконки и фоны
│   │   │   │   └── xml/                      # Конфигурация виджетов
│   │   │   └── AndroidManifest.xml
│   │   └── build.gradle.kts
│   └── ...
└── README.md
```

---

## 🚀 Установка и запуск

### Требования
- Android Studio Arctic Fox или новее
- Android SDK 24+ (Android 7.0)
- Kotlin 1.9+

### Шаги установки

1. **Клонируйте репозиторий:**
```bash
git clone https://github.com/Omasy4S/TangleApp.git
cd TangleApp
```

2. **Откройте проект в Android Studio:**
   - File → Open → Выберите папку проекта

3. **Синхронизируйте Gradle:**
   - Android Studio автоматически предложит синхронизацию
   - Или вручную: File → Sync Project with Gradle Files

4. **Запустите приложение:**
   - Подключите Android-устройство или запустите эмулятор
   - Нажмите Run (▶️) или Shift+F10

---

## 📖 Использование

### Создание проекта
1. На главном экране нажмите кнопку **"+"**
2. Введите название проекта
3. Добавьте счётчики кнопкой **"Добавить счётчик"**

### Работа со счётчиками
- **Увеличить/уменьшить:** Кнопки **+** и **−**
- **Изменить напрямую:** Клик по числу
- **Удалить счётчик:** Кнопка **×** справа от названия

### Добавление схем
1. Перейдите в раздел **"Схемы"**
2. Нажмите **"Добавить схему"**
3. Выберите:
   - **Фото** - загрузить изображение
   - **PDF** - загрузить PDF-файл
   - **Заметка** - создать текстовую заметку

### Использование калькулятора
1. Перейдите в раздел **"Калькулятор"**
2. Введите параметры текущего образца
3. Введите желаемые размеры
4. Получите точное количество петель и рядов

### Настройка темы
1. Перейдите в раздел **"Настройки"**
2. Выберите готовую тему или настройте цвета вручную
3. Нажмите на цветной круг для выбора цвета
4. Выберите иконку для проектов

### Добавление виджета
1. Долгое нажатие на главном экране Android
2. Выберите **"Виджеты"**
3. Найдите **"Счётчик вязания"**
4. Перетащите на экран
5. Выберите счётчик из списка

---

## 🎨 Настройка цветов

Приложение позволяет настроить 7 цветовых параметров:

| Параметр | Описание |
|----------|----------|
| **Основной** | Цвет кнопок и выделения |
| **Вторичный** | Фон карточек и секций |
| **Текст** | Цвет всего текста |
| **Фон** | Основной фон приложения |
| **Счетчик** | Фон счётчиков |
| **Кнопки** | Цвет кнопок счётчиков |
| **Акцент** | Цвет удаления и акцентов |

---

### Сборка APK

**Debug версия:**
```bash
./gradlew assembleDebug
```

**Release версия:**
```bash
./gradlew assembleRelease
```

APK будет находиться в: `app/build/outputs/apk/`

---

## 📝 Changelog

### Version 1.0.0 (Current)
- ✅ Управление проектами и счётчиками
- ✅ Калькулятор размеров
- ✅ Галерея схем (фото, PDF, заметки)
- ✅ Настройка цветовой темы
- ✅ Виджет на главный экран
- ✅ Оптимизированный и документированный код

---

## 🐛 Сообщить о проблеме

Нашли баг? Создайте [Issue](https://github.com/Omasy4S/TangleApp/issues) с описанием:
- Шаги для воспроизведения
- Ожидаемое поведение
- Фактическое поведение
- Скриншоты (если применимо)
- Версия Android

---

## 📄 Лицензия

Этот проект распространяется под лицензией MIT. Подробности в файле [LICENSE](LICENSE).

```
MIT License

Copyright (c) 2025 Omasy4S

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 👤 Автор

**Omasy4S**

- GitHub: [@Omasy4S](https://github.com/Omasy4S)
- Email: poshlivy@yandex.ru
- Telegram: [@omyyomyyy](https://t.me/omyyomyyy)

---

## 💖 Благодарности

- Спасибо всем вязальщицам, которые вдохновили на создание этого приложения
- Спасибо сообществу Android разработчиков за помощь и поддержку
- Особая благодарность за идеи и тестирование приложения

---

## ⭐ Поддержать проект

Если приложение оказалось полезным, поставьте ⭐ на GitHub!

Это мотивирует продолжать разработку и добавлять новые функции.

---

<div align="center">

**Сделано с 💜 для вязальщиц**

[⬆ Наверх](#-tangleapp---приложение-для-вязания)

</div>
