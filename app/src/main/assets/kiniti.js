// ============================================================================
// КЛУБОК - Приложение для вязания
// ============================================================================

// ============================================================================
// 1. ГЛОБАЛЬНЫЕ ПЕРЕМЕННЫЕ И КОНСТАНТЫ
// ============================================================================

// Безопасный парсинг JSON с fallback
function safeParse(json, fallback = []) {
    try {
        return JSON.parse(json);
    } catch {
        return fallback;
    }
}

// Глобальные данные приложения
window.projects = safeParse(localStorage.getItem('knittingProjects')) || [];
let schemes = safeParse(localStorage.getItem('knittingSchemes')) || [];
let currentProjectIndex = -1;
window.isProjectModalOpen = false;

// Иконка проекта по умолчанию
const DEFAULT_PROJECT_ICON = '🧶';

// Палитры цветов
const COLOR_PRESETS = {
    light: {
        '--primary': '#fcd9b8',
        '--secondary': '#fef5ec',
        '--accent': '#f8cdda',
        '--dark': '#5d4a66',
        '--light': '#fef7ff',
        '--counter': '#fef5ec',
        '--counter-btn': '#fcd9b8',
    },
    dark: {
        '--primary': '#5432d1', // Яркий акцент для обводок
        '--secondary': '#1E1B29', // Темный фон карточек
        '--accent': '#9e70b5', // Фиолетовый акцент
        '--dark': '#9e70b5', // Цвет текста (светло-фиолетовый)
        '--light': '#2a2636', // Светлее чем secondary для кнопок
        '--counter': '#2a2636', // Фон счетчиков
        '--counter-btn': '#3d2f5a', // Темнее для кнопок в модалках
    }
};

// ============================================================================
// 2. УТИЛИТЫ
// ============================================================================

/**
 * Экранирование HTML для безопасного отображения
 */
function escapeHtml(text) {
    return text
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

/**
 * Переключение между секциями приложения
 */
function showSection(sectionId) {
    document.querySelectorAll('.section').forEach(section => {
        section.style.display = 'none';
    });
    document.getElementById(sectionId).style.display = 'block';
}

// ============================================================================
// 3. УПРАВЛЕНИЕ ПРОЕКТАМИ
// ============================================================================

/**
 * Отрисовка списка проектов
 */
function renderProjects() {
    const grid = document.getElementById('projectsGrid');
    grid.innerHTML = '';

    // Кнопка "Добавить проект"
    const addProjectWrapper = document.createElement('div');
    addProjectWrapper.className = 'project-wrapper';
    addProjectWrapper.innerHTML = `
        <div class="project-card add-project">
            <div class="project-icon">+</div>
        </div>
    `;
    addProjectWrapper.querySelector('.project-card').onclick = createNewProject;
    grid.appendChild(addProjectWrapper);

    // Отрисовка существующих проектов
    projects.forEach((project, index) => {
        const wrapper = document.createElement('div');
        wrapper.className = 'project-wrapper';
        
        const icon = localStorage.getItem('customProjectIcon') || DEFAULT_PROJECT_ICON;
        wrapper.innerHTML = `
            <div class="project-card">
                <div class="project-icon">${icon}</div>
                <div class="project-name">${escapeHtml(project.name)}</div>
            </div>
            <button class="delete-project-card" onclick="deleteProjectCard(${index}, event)">×</button>
        `;
        
        const card = wrapper.querySelector('.project-card');
        card.onclick = (e) => {
            if (!e.target.closest('.delete-project-card')) {
                openProject(index);
            }
        };
        
        grid.appendChild(wrapper);
    });
}

/**
 * Создание нового проекта
 */
function createNewProject() {
    projects.push({ 
        name: 'Новый проект', 
        counters: [] 
    });
    saveProjects();
    openProject(projects.length - 1);
}

/**
 * Открытие модального окна проекта
 */
function openProject(index) {
    currentProjectIndex = index;
    const project = projects[index];
    
    document.getElementById('projectTitleInput').value = project.name;
    renderCounters();
    document.getElementById('projectModal').style.display = 'flex';
    window.isProjectModalOpen = true;
}

/**
 * Закрытие модального окна проекта
 */
function closeModal() {
    if (currentProjectIndex !== -1) {
        const newName = document.getElementById('projectTitleInput').value.trim();
        if (newName) {
            projects[currentProjectIndex].name = newName;
        }
        
        // Удаляем проект, если в нём нет счётчиков
        if (projects[currentProjectIndex].counters.length === 0) {
            projects.splice(currentProjectIndex, 1);
        }
        
        saveProjects();
        currentProjectIndex = -1;
    }
    
    document.getElementById('projectModal').style.display = 'none';
    window.isProjectModalOpen = false;
    renderProjects();
}

/**
 * Удаление проекта из списка
 */
function deleteProjectCard(index, event) {
    event.stopPropagation();
    
    if (confirm('Удалить этот проект?')) {
        projects.splice(index, 1);
        saveProjects();
        renderProjects();
    }
}

/**
 * Удаление текущего открытого проекта
 */
function deleteProject() {
    if (currentProjectIndex === -1 || currentProjectIndex >= projects.length) return;
    
    if (confirm('Ты уверена, что хочешь удалить этот проект? Все данные будут потеряны.')) {
        projects.splice(currentProjectIndex, 1);
        saveProjects();
        closeModal();
    }
}

/**
 * Сохранение проектов в localStorage и Android
 */
function saveProjects() {
    const json = JSON.stringify(projects);
    localStorage.setItem('knittingProjects', json);
    
    // Отправка данных в Android (если доступно)
    if (typeof Android !== 'undefined' && Android.saveProjects) {
        Android.saveProjects(json);
    }
}

// ============================================================================
// 4. УПРАВЛЕНИЕ СЧЁТЧИКАМИ
// ============================================================================

/**
 * Отрисовка счётчиков текущего проекта
 */
window.renderCounters = function() {
    const list = document.getElementById('countersList');
    if (!list || currentProjectIndex < 0 || currentProjectIndex >= projects.length) return;
    
    list.innerHTML = '';
    const project = projects[currentProjectIndex];
    
    project.counters.forEach((counter, idx) => {
        const counterDiv = document.createElement('div');
        counterDiv.className = 'counter-item';
        counterDiv.innerHTML = `
            <div class="counter-header">
                <input type="text" class="counter-name" 
                       value="${escapeHtml(counter.name)}" 
                       onblur="updateCounterName(${idx}, this.value)">
                <button class="counter-btn" onclick="removeCounter(${idx})">×</button>
            </div>
            <div class="counter-controls">
                <div class="control-group">
                    <div class="control-label"><i>🧶</i> Петли</div>
                    <div class="control-value">
                        <button class="counter-btn" onclick="decrementStitch(${idx})">−</button>
                        <div class="counter-value" id="stitch-${idx}" onclick="editStitch(${idx})">
                            ${counter.stitches}
                        </div>
                        <button class="counter-btn" onclick="incrementStitch(${idx})">+</button>
                    </div>
                </div>
                <div class="control-group">
                    <div class="control-label"><i>📏</i> Ряды</div>
                    <div class="control-value">
                        <button class="counter-btn" onclick="decrementRow(${idx})">−</button>
                        <div class="counter-value" id="row-${idx}" onclick="editRow(${idx})">
                            ${counter.rows}
                        </div>
                        <button class="counter-btn" onclick="incrementRow(${idx})">+</button>
                    </div>
                </div>
            </div>
        `;
        list.appendChild(counterDiv);
    });
};

/**
 * Добавление нового счётчика
 */
function addCounter() {
    if (currentProjectIndex === -1) return;
    
    projects[currentProjectIndex].counters.push({ 
        name: 'Новый счётчик', 
        stitches: 0, 
        rows: 0 
    });
    
    saveProjects();
    renderCounters();
}

/**
 * Обновление имени счётчика
 */
function updateCounterName(index, value) {
    if (value.trim()) {
        projects[currentProjectIndex].counters[index].name = value.trim();
        saveProjects();
    }
}

/**
 * Удаление счётчика
 */
function removeCounter(index) {
    if (!confirm('Удалить этот счётчик?')) return;
    
    projects[currentProjectIndex].counters.splice(index, 1);
    
    // Если счётчиков не осталось, удаляем проект
    if (projects[currentProjectIndex].counters.length === 0) {
        projects.splice(currentProjectIndex, 1);
        currentProjectIndex = -1;
        saveProjects();
        closeModal();
        return;
    }
    
    saveProjects();
    renderCounters();
}

// Функции изменения значений счётчиков
function incrementStitch(index) {
    projects[currentProjectIndex].counters[index].stitches++;
    document.getElementById(`stitch-${index}`).textContent = 
        projects[currentProjectIndex].counters[index].stitches;
    saveProjects();
}

function decrementStitch(index) {
    if (projects[currentProjectIndex].counters[index].stitches > 0) {
        projects[currentProjectIndex].counters[index].stitches--;
        document.getElementById(`stitch-${index}`).textContent = 
            projects[currentProjectIndex].counters[index].stitches;
        saveProjects();
    }
}

function incrementRow(index) {
    projects[currentProjectIndex].counters[index].rows++;
    document.getElementById(`row-${index}`).textContent = 
        projects[currentProjectIndex].counters[index].rows;
    saveProjects();
}

function decrementRow(index) {
    if (projects[currentProjectIndex].counters[index].rows > 0) {
        projects[currentProjectIndex].counters[index].rows--;
        document.getElementById(`row-${index}`).textContent = 
            projects[currentProjectIndex].counters[index].rows;
        saveProjects();
    }
}

/**
 * Редактирование значения петель
 */
function editStitch(index) {
    showCustomPrompt(
        'Введите новое количество петель:', 
        projects[currentProjectIndex].counters[index].stitches, 
        (value) => {
            const num = parseInt(value);
            if (!isNaN(num) && num >= 0) {
                projects[currentProjectIndex].counters[index].stitches = num;
                document.getElementById(`stitch-${index}`).textContent = num;
                saveProjects();
            } else {
                alert('Пожалуйста, введите корректное число.');
            }
        }
    );
}

/**
 * Редактирование значения рядов
 */
function editRow(index) {
    showCustomPrompt(
        'Введите новое количество рядов:', 
        projects[currentProjectIndex].counters[index].rows, 
        (value) => {
            const num = parseInt(value);
            if (!isNaN(num) && num >= 0) {
                projects[currentProjectIndex].counters[index].rows = num;
                document.getElementById(`row-${index}`).textContent = num;
                saveProjects();
            } else {
                alert('Пожалуйста, введите корректное число.');
            }
        }
    );
}

// ============================================================================
// 5. УПРАВЛЕНИЕ СХЕМАМИ И ГАЛЕРЕЕЙ
// ============================================================================

/**
 * Отрисовка схем и файлов
 */
function renderSchemes() {
    const grid = document.getElementById('galleryGrid');
    grid.innerHTML = '';
    
    schemes.forEach((scheme, index) => {
        const item = document.createElement('div');
        item.className = 'gallery-item';
        
        // PDF файл
        if (scheme.type === 'pdf') {
            item.innerHTML = `
                <div style="font-size: 2.5rem; margin-bottom: 8px;">📄</div>
                <div style="font-size: 0.75rem; word-break: break-word;">
                    ${escapeHtml(scheme.name)}
                </div>
                <button class="delete-scheme-btn" onclick="deleteScheme(${index}, event)">×</button>
            `;
            item.onclick = () => openPdfFile(scheme);
        } 
        // Текстовая заметка
        else if (scheme.type === 'note') {
            item.innerHTML = `
                <div style="font-size: 2.5rem; margin-bottom: 8px;">📝</div>
                <div style="font-size: 0.75rem; word-break: break-word;">
                    ${escapeHtml(scheme.name)}
                </div>
                <button class="delete-scheme-btn" onclick="deleteScheme(${index}, event)">×</button>
            `;
            item.onclick = () => openNote(index);
        } 
        // Изображение
        else {
            item.innerHTML = `
                <img src="${scheme.url}" alt="Схема ${index + 1}" 
                     style="width: 100%; height: 100%; object-fit: cover; border-radius: 10px;">
                <button class="delete-scheme-btn" onclick="deleteScheme(${index}, event)">×</button>
            `;
            item.querySelector('img').onclick = () => openImageModal(scheme.url);
        }
        
        grid.appendChild(item);
    });
}

/**
 * Открытие PDF файла через Android
 */
function openPdfFile(scheme) {
    if (typeof Android !== 'undefined' && Android.openFile) {
        let base64Url = scheme.url.startsWith('data:application/pdf;base64,')
            ? scheme.url
            : 'data:application/pdf;base64,' + scheme.url.replace(/^data:[^,]+,/, '');
        Android.openFile(base64Url);
    } else {
        alert('PDF можно открыть только в мобильном приложении.');
    }
}

/**
 * Добавление файла (изображение, PDF, текст)
 */
function addImage(input) {
    if (!input.files || !input.files[0]) return;
    
    const file = input.files[0];
    const reader = new FileReader();
    
    reader.onload = function(e) {
        // PDF файл
        if (file.type === 'application/pdf') {
            let base64Url = e.target.result;
            if (!base64Url.startsWith('data:application/pdf;base64,')) {
                base64Url = 'data:application/pdf;base64,' + base64Url.split(',')[1];
            }
            schemes.push({ 
                type: 'pdf', 
                url: base64Url, 
                name: file.name 
            });
        } 
        // Изображение
        else if (file.type.startsWith('image/')) {
            schemes.push({ 
                type: 'image', 
                url: e.target.result 
            });
        } 
        // Текстовый файл
        else if (file.type === 'text/plain' || file.name.toLowerCase().endsWith('.txt')) {
            schemes.push({ 
                type: 'note', 
                content: e.target.result, 
                name: file.name 
            });
        } 
        else {
            alert('Поддерживаются только изображения, PDF и текстовые файлы (.txt).');
            input.value = '';
            return;
        }
        
        saveSchemes();
        renderSchemes();
    };
    
    // Выбор метода чтения в зависимости от типа файла
    if (file.type === 'application/pdf' || file.type.startsWith('image/')) {
        reader.readAsDataURL(file);
    } else {
        reader.readAsText(file);
    }
    
    input.value = '';
}

/**
 * Удаление схемы
 */
function deleteScheme(index, event) {
    event.stopPropagation();
    
    if (confirm('Удалить этот элемент?')) {
        schemes.splice(index, 1);
        saveSchemes();
        renderSchemes();
    }
}

/**
 * Сохранение схем в localStorage
 */
function saveSchemes() {
    localStorage.setItem('knittingSchemes', JSON.stringify(schemes));
}

/**
 * Открытие изображения в модальном окне
 */
function openImageModal(imageUrl) {
    document.getElementById('modalImage').src = imageUrl;
    document.getElementById('imageModal').style.display = 'flex';
}

/**
 * Закрытие модального окна изображения
 */
function closeImageModal() {
    document.getElementById('imageModal').style.display = 'none';
}

// ============================================================================
// 6. УПРАВЛЕНИЕ ЗАМЕТКАМИ
// ============================================================================

/**
 * Открытие заметки для редактирования
 */
function openNote(index) {
    if (index < 0 || index >= schemes.length || schemes[index].type !== 'note') return;
    
    const note = schemes[index];
    const modal = document.getElementById('noteModal');
    
    modal.style.display = 'flex';
    modal.innerHTML = `
        <div class="modal-content" style="max-width: 90vw; max-height: 90vh; display: flex; flex-direction: column;">
            <span class="close-modal" onclick="closeNoteModal()" style="align-self: flex-end;">&times;</span>
            <div class="modal-title" style="padding-bottom: 8px; border-bottom: 1px solid var(--primary);">
                <span id="noteTitleDisplay" onclick="editNoteName(${index})">
                    ${escapeHtml(note.name)}
                </span>
            </div>
            <textarea id="noteContentArea" style="margin-top: 16px;">
                ${escapeHtml(note.content)}
            </textarea>
        </div>
    `;
    
    document.getElementById('noteContentArea').focus();
}

/**
 * Закрытие модального окна заметки
 */
function closeNoteModal() {
    const modal = document.getElementById('noteModal');
    
    if (modal.style.display === 'flex') {
        const textarea = document.getElementById('noteContentArea');
        const titleDisplay = document.getElementById('noteTitleDisplay');
        
        if (textarea && titleDisplay) {
            const currentNoteName = titleDisplay.textContent.trim();
            const index = schemes.findIndex(s => s.type === 'note' && s.name === currentNoteName);
            
            if (index !== -1) {
                schemes[index].content = textarea.value;
                saveSchemes();
                renderSchemes();
            }
        }
    }
    
    modal.style.display = 'none';
}

/**
 * Редактирование имени заметки
 */
function editNoteName(index) {
    if (index < 0 || index >= schemes.length || schemes[index].type !== 'note') return;
    
    const currentName = schemes[index].name;
    
    showCustomPromptForText('Введите новое имя файла:', currentName, (newName) => {
        if (newName && newName.trim()) {
            schemes[index].name = newName.trim();
            saveSchemes();
            renderSchemes();
            
            const titleDisplay = document.getElementById('noteTitleDisplay');
            if (titleDisplay) {
                titleDisplay.textContent = schemes[index].name;
            }
        }
    });
}

/**
 * Создание новой заметки
 */
function createNewNote() {
    document.getElementById('newNoteNameInput').value = 'Новая заметка';
    document.getElementById('newNoteContentInput').value = '';
    document.getElementById('createNoteModal').style.display = 'flex';
}

/**
 * Закрытие модального окна создания заметки
 */
function closeCreateNoteModal() {
    document.getElementById('createNoteModal').style.display = 'none';
}

/**
 * Сохранение новой заметки
 */
function saveNewNote() {
    let name = document.getElementById('newNoteNameInput').value.trim();
    const content = document.getElementById('newNoteContentInput').value;
    
    if (!name) {
        alert('Пожалуйста, введите имя файла.');
        return;
    }
    
    // Добавляем расширение .txt если его нет
    if (!name.toLowerCase().endsWith('.txt')) {
        name += '.txt';
    }
    
    // Проверка на существующий файл
    const existingIndex = schemes.findIndex(s => s.type === 'note' && s.name === name);
    if (existingIndex !== -1) {
        if (!confirm('Файл с таким именем уже существует. Перезаписать?')) {
            return;
        }
        schemes[existingIndex].content = content;
    } else {
        schemes.push({ 
            type: 'note', 
            name: name, 
            content: content 
        });
    }
    
    saveSchemes();
    renderSchemes();
    closeCreateNoteModal();
}

/**
 * Показать меню загрузки
 */
function showUploadMenu() {
    document.getElementById('uploadMenuModal').style.display = 'flex';
}

/**
 * Закрыть меню загрузки
 */
function closeUploadMenu() {
    document.getElementById('uploadMenuModal').style.display = 'none';
}

// ============================================================================
// 7. КАЛЬКУЛЯТОР ПЕТЕЛЬ И РЯДОВ
// ============================================================================

/**
 * Переход к следующему шагу калькулятора
 */
function nextStep() {
    const currentWidth = parseFloat(document.getElementById('currentWidth').value);
    const currentLength = parseFloat(document.getElementById('currentLength').value);
    const currentStitches = parseFloat(document.getElementById('currentStitches').value);
    const currentRows = parseFloat(document.getElementById('currentRows').value);
    
    if (isNaN(currentWidth) || isNaN(currentLength) || isNaN(currentStitches) || isNaN(currentRows) ||
        currentWidth <= 0 || currentLength <= 0 || currentStitches <= 0 || currentRows <= 0) {
        alert('Пожалуйста, заполните все поля текущих параметров корректными числами.');
        return;
    }
    
    document.getElementById('step1').style.display = 'none';
    document.getElementById('step2').style.display = 'block';
}

/**
 * Расчёт необходимых петель и рядов
 */
function calculate() {
    const currentWidth = parseFloat(document.getElementById('currentWidth').value);
    const currentLength = parseFloat(document.getElementById('currentLength').value);
    const currentStitches = parseFloat(document.getElementById('currentStitches').value);
    const currentRows = parseFloat(document.getElementById('currentRows').value);
    const desiredWidth = parseFloat(document.getElementById('desiredWidth').value);
    const desiredLength = parseFloat(document.getElementById('desiredLength').value);
    
    if (isNaN(desiredWidth) || isNaN(desiredLength) || desiredWidth <= 0 || desiredLength <= 0) {
        alert('Пожалуйста, заполните все поля желаемых параметров корректными числами.');
        return;
    }
    
    // Расчёт плотности вязания
    const stitchesPerCm = currentStitches / currentWidth;
    const rowsPerCm = currentRows / currentLength;
    
    // Расчёт требуемых значений
    const requiredStitches = Math.round(stitchesPerCm * desiredWidth);
    const requiredRows = Math.round(rowsPerCm * desiredLength);
    
    // Отображение результата
    const resultDiv = document.getElementById('calcResult');
    resultDiv.innerHTML = `
        <p>Требуется: <strong>${requiredStitches} п.</strong> и <strong>${requiredRows} р.</strong></p>
    `;
    
    document.getElementById('step2').style.display = 'none';
    document.getElementById('step3').style.display = 'block';
}

/**
 * Возврат к предыдущему шагу
 */
function backToStep2() {
    document.getElementById('step3').style.display = 'none';
    document.getElementById('step2').style.display = 'block';
}

/**
 * Сброс калькулятора
 */
function resetCalculator() {
    document.getElementById('currentWidth').value = '';
    document.getElementById('currentLength').value = '';
    document.getElementById('currentStitches').value = '';
    document.getElementById('currentRows').value = '';
    document.getElementById('desiredWidth').value = '';
    document.getElementById('desiredLength').value = '';
    document.getElementById('calcResult').innerHTML = '';
    
    document.getElementById('step3').style.display = 'none';
    document.getElementById('step2').style.display = 'none';
    document.getElementById('step1').style.display = 'block';
}

// ============================================================================
// 8. НАСТРОЙКИ ЦВЕТОВОЙ ПАЛИТРЫ
// ============================================================================

let pickrInstances = []; // Хранилище экземпляров Pickr

/**
 * Применение цветовой палитры
 */
function applyPalette(palette) {
    Object.entries(palette).forEach(([varName, value]) => {
        document.documentElement.style.setProperty(varName, value);
    });
    localStorage.setItem('customPalette', JSON.stringify(palette));
    refreshSwatches(palette);
}

/**
 * Обновление цветовых кружков
 */
function refreshSwatches(palette) {
    Object.entries(palette).forEach(([varName, value]) => {
        const swatch = document.querySelector(`.color-swatch[data-var="${varName}"]`);
        if (swatch) {
            swatch.style.backgroundColor = value;
        }
    });
}

/**
 * Инициализация Pickr для выбора цветов
 */
function initializeColorPickers() {
    // Очищаем старые экземпляры
    pickrInstances.forEach(pickr => pickr.destroyAndRemove());
    pickrInstances = [];
    
    document.querySelectorAll('.color-swatch').forEach(swatch => {
        const varName = swatch.dataset.var;
        const initial = getComputedStyle(document.documentElement)
            .getPropertyValue(varName).trim();
        
        swatch.style.backgroundColor = initial;
        
        const pickr = Pickr.create({
            el: swatch,
            theme: 'classic',
            default: initial,
            components: {
                preview: true,
                opacity: false,
                hue: true,
                interaction: { 
                    input: true, 
                    save: true, 
                    cancel: true 
                }
            },
            i18n: {
                'ui:dialog': 'Выбор цвета',
                'btn:save': 'Установить',
                'btn:cancel': 'Отмена',
                'btn:input': 'Ввести HEX',
                'lbl:hex': 'HEX'
            }
        });
        
        pickr.on('save', color => {
            const hex = color.toHEXA().toString();
            applyPalette({ [varName]: hex });
            pickr.hide();
        });
        
        pickr.on('cancel', () => pickr.hide());
        
        pickrInstances.push(pickr);
    });
}

/**
 * Инициализация кнопок выбора темы
 */
function initializeThemeButtons() {
    document.querySelectorAll('[data-palette]').forEach(btn => {
        btn.addEventListener('click', () => {
            const paletteName = btn.dataset.palette;
            applyPalette(COLOR_PRESETS[paletteName]);
        });
    });
}

/**
 * Загрузка сохранённой палитры
 */
function loadSavedPalette() {
    const savedPalette = JSON.parse(localStorage.getItem('customPalette') || '{}');
    if (Object.keys(savedPalette).length) {
        applyPalette(savedPalette);
    }
}

// ============================================================================
// 9. НАСТРОЙКИ ИКОНКИ ПРОЕКТА
// ============================================================================

/**
 * Применение иконки к проектам
 */
function applyProjectIcon(icon) {
    document.querySelectorAll('.project-card .project-icon').forEach(el => {
        if (el && !el.closest('.add-project')) {
            el.textContent = icon;
        }
    });
}

/**
 * Сохранение выбранной иконки
 */
function saveProjectIcon(icon) {
    localStorage.setItem('customProjectIcon', icon);
    applyProjectIcon(icon);
}

/**
 * Загрузка сохранённой иконки
 */
function loadProjectIcon() {
    const icon = localStorage.getItem('customProjectIcon') || DEFAULT_PROJECT_ICON;
    applyProjectIcon(icon);
    
    document.querySelectorAll('.icon-choice').forEach(btn => {
        btn.classList.toggle('selected', btn.dataset.icon === icon);
    });
}

/**
 * Инициализация кнопок выбора иконки
 */
function initializeIconButtons() {
    document.querySelectorAll('.icon-choice').forEach(btn => {
        btn.onclick = () => {
            saveProjectIcon(btn.dataset.icon);
            loadProjectIcon();
        };
    });
}

// ============================================================================
// 10. МОДАЛЬНЫЕ ОКНА И ПРОМПТЫ
// ============================================================================

/**
 * Кастомный промпт для ввода числа
 */
function showCustomPrompt(title, defaultValue, callback) {
    const modal = document.createElement('div');
    modal.style.cssText = `
        position: fixed; top: 0; left: 0; width: 100%; height: 100%;
        background: rgba(0, 0, 0, 0.5); display: flex;
        justify-content: center; align-items: center;
        z-index: 9999; padding: 16px;
    `;
    
    const dialog = document.createElement('div');
    dialog.style.cssText = `
        background: white; border-radius: 12px; padding: 20px;
        width: 90%; max-width: 300px; box-shadow: 0 10px 30px rgba(0,0,0,0.2);
        display: flex; flex-direction: column; gap: 16px;
    `;
    
    const titleEl = document.createElement('h3');
    titleEl.textContent = title;
    titleEl.style.cssText = 'margin: 0 0 12px; font-weight: 600; color: #5d4a66;';
    dialog.appendChild(titleEl);
    
    const input = document.createElement('input');
    input.type = 'number';
    input.value = defaultValue;
    input.style.cssText = `
        width: 100%; padding: 10px; border: 2px solid #eee;
        border-radius: 8px; font-size: 1rem; outline: none;
        transition: border-color 0.3s; min-height: 40px;
    `;
    input.focus();
    dialog.appendChild(input);
    
    const buttonContainer = document.createElement('div');
    buttonContainer.style.cssText = 'display: flex; gap: 12px; justify-content: flex-end;';
    
    const cancelBtn = document.createElement('button');
    cancelBtn.textContent = 'Отменить';
    cancelBtn.style.cssText = `
        background: #fcd9b8; color: #5d4a66; border: none;
        padding: 8px 16px; border-radius: 8px; cursor: pointer; font-weight: 600;
    `;
    cancelBtn.onclick = () => document.body.removeChild(modal);
    buttonContainer.appendChild(cancelBtn);
    
    const okBtn = document.createElement('button');
    okBtn.textContent = 'ОК';
    okBtn.style.cssText = `
        background: #5d4a66; color: white; border: none;
        padding: 8px 16px; border-radius: 8px; cursor: pointer; font-weight: 600;
    `;
    okBtn.onclick = () => {
        callback(input.value);
        document.body.removeChild(modal);
    };
    buttonContainer.appendChild(okBtn);
    
    dialog.appendChild(buttonContainer);
    modal.appendChild(dialog);
    document.body.appendChild(modal);
    
    modal.onclick = (e) => {
        if (e.target === modal) document.body.removeChild(modal);
    };
    dialog.onclick = (e) => e.stopPropagation();
}

/**
 * Кастомный промпт для ввода текста
 */
function showCustomPromptForText(title, defaultValue, callback) {
    const modal = document.createElement('div');
    modal.style.cssText = `
        position: fixed; top: 0; left: 0; width: 100%; height: 100%;
        background: rgba(0, 0, 0, 0.5); display: flex;
        justify-content: center; align-items: center;
        z-index: 9999; padding: 16px;
    `;
    
    const dialog = document.createElement('div');
    dialog.style.cssText = `
        background: white; border-radius: 12px; padding: 20px;
        width: 90%; max-width: 400px; max-height: 80vh;
        box-shadow: 0 10px 30px rgba(0,0,0,0.2);
        display: flex; flex-direction: column; gap: 16px;
    `;
    
    const titleEl = document.createElement('h3');
    titleEl.textContent = title;
    titleEl.style.cssText = 'margin: 0 0 12px; font-weight: 600; color: #5d4a66;';
    dialog.appendChild(titleEl);
    
    const textarea = document.createElement('textarea');
    textarea.value = defaultValue;
    textarea.style.cssText = `
        width: 100%; height: 150px; padding: 10px;
        border: 2px solid #eee; border-radius: 8px;
        font-size: 1rem; outline: none; transition: border-color 0.3s;
        resize: vertical;
    `;
    textarea.focus();
    dialog.appendChild(textarea);
    
    const buttonContainer = document.createElement('div');
    buttonContainer.style.cssText = 'display: flex; gap: 12px; justify-content: flex-end;';
    
    const cancelBtn = document.createElement('button');
    cancelBtn.textContent = 'Отменить';
    cancelBtn.style.cssText = `
        background: #fcd9b8; color: #5d4a66; border: none;
        padding: 8px 16px; border-radius: 8px; cursor: pointer; font-weight: 600;
    `;
    cancelBtn.onclick = () => document.body.removeChild(modal);
    buttonContainer.appendChild(cancelBtn);
    
    const okBtn = document.createElement('button');
    okBtn.textContent = 'ОК';
    okBtn.style.cssText = `
        background: #5d4a66; color: white; border: none;
        padding: 8px 16px; border-radius: 8px; cursor: pointer; font-weight: 600;
    `;
    okBtn.onclick = () => {
        callback(textarea.value);
        document.body.removeChild(modal);
    };
    buttonContainer.appendChild(okBtn);
    
    dialog.appendChild(buttonContainer);
    modal.appendChild(dialog);
    document.body.appendChild(modal);
    
    modal.onclick = (e) => {
        if (e.target === modal) document.body.removeChild(modal);
    };
    dialog.onclick = (e) => e.stopPropagation();
}

// ============================================================================
// 11. ИНТЕГРАЦИЯ С ANDROID
// ============================================================================

/**
 * Обновление проектов из Android
 */
window.updateProjectsFromAndroid = function(jsonString) {
    try {
        const parsed = JSON.parse(jsonString);
        if (Array.isArray(parsed)) {
            window.projects = parsed;
            localStorage.setItem('knittingProjects', jsonString);
            renderProjects();
            
            if (window.isProjectModalOpen && typeof window.renderCounters === 'function') {
                window.renderCounters();
            }
        } else {
            console.warn('Received non-array data from Android:', jsonString);
        }
    } catch (e) {
        console.error('Failed to parse projects from Android:', e, jsonString);
    }
};

// ============================================================================
// 12. ИНИЦИАЛИЗАЦИЯ ПРИЛОЖЕНИЯ
// ============================================================================

/**
 * Главная функция инициализации
 */
document.addEventListener('DOMContentLoaded', function() {
    // Отрисовка основных элементов
    renderProjects();
    renderSchemes();
    showSection('projects');
    
    // Загрузка настроек
    loadSavedPalette();
    loadProjectIcon();
    
    // Инициализация кнопок и пикеров
    initializeThemeButtons();
    initializeIconButtons();
    initializeColorPickers();
});
