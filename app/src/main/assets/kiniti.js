function safeParse(json, fallback = []) {
    try { return JSON.parse(json) } catch { return fallback }
}
window.projects = safeParse(localStorage.getItem('knittingProjects')) || [];
let schemes = safeParse(localStorage.getItem('knittingSchemes')) || [];
let currentProjectIndex = -1;
window.isProjectModalOpen = false;
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
document.addEventListener('DOMContentLoaded', function() {
    renderProjects();
    renderSchemes();
    showSection('projects');
});
function showSection(sectionId) {
    document.querySelectorAll('.section').forEach(section => {
        section.style.display = 'none';
    });
    document.getElementById(sectionId).style.display = 'block';
}
function renderProjects() {
    const grid = document.getElementById('projectsGrid');
    grid.innerHTML = '';
    const addProjectWrapper = document.createElement('div');
    addProjectWrapper.className = 'project-wrapper';
    addProjectWrapper.innerHTML = `<div class="project-card add-project"><div class="project-icon">+</div></div>`;
    addProjectWrapper.querySelector('.project-card').onclick = createNewProject;
    grid.appendChild(addProjectWrapper);
    projects.forEach((project, index) => {
        const wrapper = document.createElement('div');
        wrapper.className = 'project-wrapper';
        wrapper.innerHTML = `
            <div class="project-card">
                <div class="project-icon">${localStorage.getItem('customProjectIcon') || '🧶'}</div>
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
function createNewProject() {
    projects.push({ name: 'Новый проект', counters: [] });
    saveProjects();
    openProject(projects.length - 1);
}
function openProject(index) {
    currentProjectIndex = index;
    const project = projects[index];
    document.getElementById('projectTitleInput').value = project.name;
    renderCounters();
    document.getElementById('projectModal').style.display = 'flex';
    window.isProjectModalOpen = true;
}
function closeModal() {
    if (currentProjectIndex !== -1) {
        const newName = document.getElementById('projectTitleInput').value.trim();
        if (newName) projects[currentProjectIndex].name = newName;
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
                <input type="text" class="counter-name" value="${escapeHtml(counter.name)}" onblur="updateCounterName(${idx}, this.value)">
                <button class="counter-btn" onclick="removeCounter(${idx})">×</button>
            </div>
            <div class="counter-controls">
                <div class="control-group">
                    <div class="control-label"><i>🧶</i> Петли</div>
                    <div class="control-value">
                        <button class="counter-btn" onclick="decrementStitch(${idx})">−</button>
                        <div class="counter-value" id="stitch-${idx}" onclick="editStitch(${idx})">${counter.stitches}</div>
                        <button class="counter-btn" onclick="incrementStitch(${idx})">+</button>
                    </div>
                </div>
                <div class="control-group">
                    <div class="control-label"><i>📏</i> Ряды</div>
                    <div class="control-value">
                        <button class="counter-btn" onclick="decrementRow(${idx})">−</button>
                        <div class="counter-value" id="row-${idx}" onclick="editRow(${idx})">${counter.rows}</div>
                        <button class="counter-btn" onclick="incrementRow(${idx})">+</button>
                    </div>
                </div>
            </div>
        `;
        list.appendChild(counterDiv);
    });
};
function escapeHtml(text) {
    return text
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}
function addCounter() {
    if (currentProjectIndex === -1) return;
    projects[currentProjectIndex].counters.push({ name: 'Новый счётчик', stitches: 0, rows: 0 });
    saveProjects();
    renderCounters();
}
function updateCounterName(index, value) {
    if (value.trim()) {
        projects[currentProjectIndex].counters[index].name = value.trim();
        saveProjects();
    }
}
function removeCounter(index) {
    if (confirm('Удалить этот счётчик?')) {
        projects[currentProjectIndex].counters.splice(index, 1);
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
}
function incrementStitch(index) {
    projects[currentProjectIndex].counters[index].stitches++;
    document.getElementById(`stitch-${index}`).textContent = projects[currentProjectIndex].counters[index].stitches;
    saveProjects();
}
function decrementStitch(index) {
    if (projects[currentProjectIndex].counters[index].stitches > 0) {
        projects[currentProjectIndex].counters[index].stitches--;
        document.getElementById(`stitch-${index}`).textContent = projects[currentProjectIndex].counters[index].stitches;
        saveProjects();
    }
}
function incrementRow(index) {
    projects[currentProjectIndex].counters[index].rows++;
    document.getElementById(`row-${index}`).textContent = projects[currentProjectIndex].counters[index].rows;
    saveProjects();
}
function decrementRow(index) {
    if (projects[currentProjectIndex].counters[index].rows > 0) {
        projects[currentProjectIndex].counters[index].rows--;
        document.getElementById(`row-${index}`).textContent = projects[currentProjectIndex].counters[index].rows;
        saveProjects();
    }
}
function editStitch(index) {
    showCustomPrompt('Введите новое количество петель:', projects[currentProjectIndex].counters[index].stitches, (value) => {
        const num = parseInt(value);
        if (!isNaN(num) && num >= 0) {
            projects[currentProjectIndex].counters[index].stitches = num;
            document.getElementById(`stitch-${index}`).textContent = num;
            saveProjects();
        } else {
            alert('Пожалуйста, введите корректное число.');
        }
    });
}
function editRow(index) {
    showCustomPrompt('Введите новое количество рядов:', projects[currentProjectIndex].counters[index].rows, (value) => {
        const num = parseInt(value);
        if (!isNaN(num) && num >= 0) {
            projects[currentProjectIndex].counters[index].rows = num;
            document.getElementById(`row-${index}`).textContent = num;
            saveProjects();
        } else {
            alert('Пожалуйста, введите корректное число.');
        }
    });
}
function deleteProject() {
    if (currentProjectIndex === -1 || currentProjectIndex >= projects.length) return;
    if (confirm('Ты уверена, что хочешь удалить этот проект? Все данные будут потеряны.')) {
        projects.splice(currentProjectIndex, 1);
        saveProjects();
        closeModal();
    }
}
function deleteProjectCard(index, event) {
    event.stopPropagation();
    if (confirm('Удалить этот проект?')) {
        projects.splice(index, 1);
        saveProjects();
        renderProjects();
    }
}
function saveProjects() {
    const json = JSON.stringify(projects);
    localStorage.setItem('knittingProjects', json);
    if (typeof Android !== 'undefined' && Android.saveProjects) {
        Android.saveProjects(json);
    }
}
function renderSchemes() {
    const grid = document.getElementById('galleryGrid');
    grid.innerHTML = '';
    schemes.forEach((scheme, index) => {
        const item = document.createElement('div');
        item.className = 'gallery-item';
        if (scheme.type === 'pdf') {
            item.innerHTML = `
                <div style="font-size: 2.5rem; margin-bottom: 8px;">📄</div>
                <div style="font-size: 0.75rem; word-break: break-word;">${escapeHtml(scheme.name)}</div>
                <button class="delete-scheme-btn" onclick="deleteScheme(${index}, event)">×</button>
            `;
            item.onclick = () => {
                if (typeof Android !== 'undefined' && Android.openFile) {
                    let base64Url = scheme.url.startsWith('data:application/pdf;base64,')
                        ? scheme.url
                        : 'data:application/pdf;base64,' + scheme.url.replace(/^data:[^,]+,/, '');
                    Android.openFile(base64Url);
                } else {
                    alert('PDF можно открыть только в мобильном приложении.');
                }
            };
        } else if (scheme.type === 'note') {
            item.innerHTML = `
                <div style="font-size: 2.5rem; margin-bottom: 8px;">📝</div>
                <div style="font-size: 0.75rem; word-break: break-word;">${escapeHtml(scheme.name)}</div>
                <button class="delete-scheme-btn" onclick="deleteScheme(${index}, event)">×</button>
            `;
            item.onclick = () => openNote(index);
        } else {
            item.innerHTML = `
                <img src="${scheme.url}" alt="Схема ${index + 1}" style="width: 100%; height: 100%; object-fit: cover; border-radius: 10px;">
                <button class="delete-scheme-btn" onclick="deleteScheme(${index}, event)">×</button>
            `;
            item.querySelector('img').onclick = () => openImageModal(scheme.url);
        }
        grid.appendChild(item);
    });
}
function addImage(input) {
    if (input.files && input.files[0]) {
        const file = input.files[0];
        const reader = new FileReader();
        reader.onload = function(e) {
            if (file.type === 'application/pdf') {
                let base64Url = e.target.result;
                if (!base64Url.startsWith('data:application/pdf;base64,')) {
                    base64Url = 'data:application/pdf;base64,' + base64Url.split(',')[1];
                }
                schemes.push({ type: 'pdf', url: base64Url, name: file.name });
            } else if (file.type.startsWith('image/')) {
                schemes.push({ type: 'image', url: e.target.result });
            } else if (file.type === 'text/plain' || file.name.toLowerCase().endsWith('.txt')) {
                schemes.push({ type: 'note', content: e.target.result, name: file.name });
            } else {
                alert('Поддерживаются только изображения, PDF и текстовые файлы (.txt).');
            }
            saveSchemes(); renderSchemes();
        };
        if (file.type === 'application/pdf' || file.type.startsWith('image/')) {
            reader.readAsDataURL(file);
        } else {
            reader.readAsText(file);
        }
    }
    input.value = '';
}
function deleteScheme(index, event) {
    event.stopPropagation();
    if (confirm('Удалить этот элемент?')) {
        schemes.splice(index, 1);
        saveSchemes();
        renderSchemes();
    }
}

// Заметки
function openNote(index) {
    if (index < 0 || index >= schemes.length || schemes[index].type !== 'note') return;
    const note = schemes[index];
    const modal = document.getElementById('noteModal');
    modal.style.display = 'flex';
    modal.innerHTML = `
        <div class="modal-content" style="max-width: 90vw; max-height: 90vh; display: flex; flex-direction: column;">
            <span class="close-modal" onclick="closeNoteModal()" style="align-self: flex-end;">&times;</span>
            <div class="modal-title" style="padding-bottom: 8px; border-bottom: 1px solid var(--primary);">
                <span id="noteTitleDisplay" onclick="editNoteName(${index})">${escapeHtml(note.name)}</span>
            </div>
            <textarea id="noteContentArea" style="margin-top: 16px;">${escapeHtml(note.content)}</textarea>
        </div>
    `;
    document.getElementById('noteContentArea').focus();
}
function closeNoteModal() {
    const modal = document.getElementById('noteModal');
    if (modal.style.display === 'flex') {
        const textarea = document.getElementById('noteContentArea');
        const titleDisplay = document.getElementById('noteTitleDisplay');
        if (textarea && titleDisplay) {
            const currentNoteName = titleDisplay.textContent;
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
function editNoteName(index) {
    if (index < 0 || index >= schemes.length || schemes[index].type !== 'note') return;
    const currentName = schemes[index].name;
    showCustomPromptForText('Введите новое имя файла:', currentName, (newName) => {
        if (newName && newName.trim()) {
            schemes[index].name = newName.trim();
            saveSchemes();
            renderSchemes();
            const titleDisplay = document.getElementById('noteTitleDisplay');
            if (titleDisplay) { titleDisplay.textContent = escapeHtml(schemes[index].name); }
        }
    });
}
function showCustomPromptForText(title, defaultValue, callback) {
    const modal = document.createElement('div');
    modal.style.position = 'fixed';
    modal.style.top = '0';
    modal.style.left = '0';
    modal.style.width = '100%';
    modal.style.height = '100%';
    modal.style.backgroundColor = 'rgba(0, 0, 0, 0.5)';
    modal.style.display = 'flex';
    modal.style.justifyContent = 'center';
    modal.style.alignItems = 'center';
    modal.style.zIndex = '9999';
    modal.style.padding = '16px';

    const dialog = document.createElement('div');
    dialog.style.backgroundColor = 'white';
    dialog.style.borderRadius = '12px';
    dialog.style.padding = '20px';
    dialog.style.width = '90%';
    dialog.style.maxWidth = '400px';
    dialog.style.maxHeight = '80vh';
    dialog.style.boxShadow = '0 10px 30px rgba(0,0,0,0.2)';
    dialog.style.display = 'flex';
    dialog.style.flexDirection = 'column';
    dialog.style.gap = '16px';

    const titleEl = document.createElement('h3');
    titleEl.textContent = title;
    titleEl.style.margin = '0 0 12px';
    titleEl.style.fontWeight = '600';
    titleEl.style.color = '#5d4a66';
    dialog.appendChild(titleEl);

    const textarea = document.createElement('textarea');
    textarea.value = defaultValue;
    textarea.style.width = '100%';
    textarea.style.height = '150px';
    textarea.style.padding = '10px';
    textarea.style.border = '2px solid #eee';
    textarea.style.borderRadius = '8px';
    textarea.style.fontSize = '1rem';
    textarea.style.outline = 'none';
    textarea.style.transition = 'border-color 0.3s';
    textarea.style.resize = 'vertical';
    textarea.focus();
    dialog.appendChild(textarea);

    const buttonContainer = document.createElement('div');
    buttonContainer.style.display = 'flex';
    buttonContainer.style.gap = '12px';
    buttonContainer.style.justifyContent = 'flex-end';

    const cancelBtn = document.createElement('button');
    cancelBtn.textContent = 'Отменить';
    cancelBtn.style.background = '#fcd9b8';
    cancelBtn.style.color = '#5d4a66';
    cancelBtn.style.border = 'none';
    cancelBtn.style.padding = '8px 16px';
    cancelBtn.style.borderRadius = '8px';
    cancelBtn.style.cursor = 'pointer';
    cancelBtn.style.fontWeight = '600';
    cancelBtn.onclick = () => { document.body.removeChild(modal); };
    buttonContainer.appendChild(cancelBtn);

    const okBtn = document.createElement('button');
    okBtn.textContent = 'ОК';
    okBtn.style.background = '#5d4a66';
    okBtn.style.color = 'white';
    okBtn.style.border = 'none';
    okBtn.style.padding = '8px 16px';
    okBtn.style.borderRadius = '8px';
    okBtn.style.cursor = 'pointer';
    okBtn.style.fontWeight = '600';
    okBtn.onclick = () => {
        callback(textarea.value);
        document.body.removeChild(modal);
    };
    buttonContainer.appendChild(okBtn);

    dialog.appendChild(buttonContainer);
    modal.appendChild(dialog);
    document.body.appendChild(modal);

    modal.onclick = (e) => { if (e.target === modal) { document.body.removeChild(modal); } };
    dialog.onclick = (e) => e.stopPropagation();
}

// Галерея, открытие картинок
function saveSchemes() { localStorage.setItem('knittingSchemes', JSON.stringify(schemes)); }
function openImageModal(imageUrl) {
    document.getElementById('modalImage').src = imageUrl;
    document.getElementById('imageModal').style.display = 'flex';
}
function closeImageModal() {
    document.getElementById('imageModal').style.display = 'none';
}


// Калькулятор
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
    const stitchesPerCm = currentStitches / currentWidth;
    const rowsPerCm = currentRows / currentLength;
    const requiredStitches = Math.round(stitchesPerCm * desiredWidth);
    const requiredRows = Math.round(rowsPerCm * desiredLength);
    const resultDiv = document.getElementById('calcResult');
    resultDiv.innerHTML = `
        <p>Требуется: <strong>${requiredStitches} п.</strong> и <strong>${requiredRows} р.</strong></p>
    `;
    document.getElementById('step2').style.display = 'none';
    document.getElementById('step3').style.display = 'block';
}
function backToStep2() {
    document.getElementById('step3').style.display = 'none';
    document.getElementById('step2').style.display = 'block';
}
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

// Модальные окна-upload
function createNewNote() {
    document.getElementById('newNoteNameInput').value = 'Новая заметка';
    document.getElementById('newNoteContentInput').value = '';
    document.getElementById('createNoteModal').style.display = 'flex';
}
function closeCreateNoteModal() {
    document.getElementById('createNoteModal').style.display = 'none';
}
function saveNewNote() {
    let name = document.getElementById('newNoteNameInput').value.trim();
    if (!name.toLowerCase().endsWith('.txt')) {
        name += '.txt';
    }
    const content = document.getElementById('newNoteContentInput').value;
    if (!name) {
        alert('Пожалуйста, введите имя файла.');
        return;
    }
    if (!name.toLowerCase().endsWith('.txt')) {
        alert('Имя файла должно заканчиваться на .txt');
        return;
    }
    const existingIndex = schemes.findIndex(s => s.type === 'note' && s.name === name);
    if (existingIndex !== -1) {
        if (!confirm('Файл с таким именем уже существует. Перезаписать?')) {
            return;
        }
        schemes[existingIndex].content = content;
    } else {
        schemes.push({ type: 'note', name: name, content: content });
    }
    saveSchemes();
    renderSchemes();
    closeCreateNoteModal();
}
function showUploadMenu() {
    document.getElementById('uploadMenuModal').style.display = 'flex';
}
function closeUploadMenu() {
    document.getElementById('uploadMenuModal').style.display = 'none';
}
function showCustomPrompt(title, defaultValue, callback) {
    const modal = document.createElement('div');
    modal.style.position = 'fixed';
    modal.style.top = '0';
    modal.style.left = '0';
    modal.style.width = '100%';
    modal.style.height = '100%';
    modal.style.backgroundColor = 'rgba(0, 0, 0, 0.5)';
    modal.style.display = 'flex';
    modal.style.justifyContent = 'center';
    modal.style.alignItems = 'center';
    modal.style.zIndex = '9999';
    modal.style.padding = '16px';

    const dialog = document.createElement('div');
    dialog.style.backgroundColor = 'white';
    dialog.style.borderRadius = '12px';
    dialog.style.padding = '20px';
    dialog.style.width = '90%';
    dialog.style.maxWidth = '300px';
    dialog.style.boxShadow = '0 10px 30px rgba(0,0,0,0.2)';
    dialog.style.display = 'flex';
    dialog.style.flexDirection = 'column';
    dialog.style.gap = '16px';

    const titleEl = document.createElement('h3');
    titleEl.textContent = title;
    titleEl.style.margin = '0 0 12px';
    titleEl.style.fontWeight = '600';
    titleEl.style.color = '#5d4a66';
    dialog.appendChild(titleEl);

    const input = document.createElement('input');
    input.type = 'number';
    input.value = defaultValue;
    input.style.width = '100%';
    input.style.padding = '10px';
    input.style.border = '2px solid #eee';
    input.style.borderRadius = '8px';
    input.style.fontSize = '1rem';
    input.style.outline = 'none';
    input.style.transition = 'border-color 0.3s';
    input.style.minHeight = '40px';
    input.focus();
    dialog.appendChild(input);

    const buttonContainer = document.createElement('div');
    buttonContainer.style.display = 'flex';
    buttonContainer.style.gap = '12px';
    buttonContainer.style.justifyContent = 'flex-end';

    const cancelBtn = document.createElement('button');
    cancelBtn.textContent = 'Отменить';
    cancelBtn.style.background = '#fcd9b8';
    cancelBtn.style.color = '#5d4a66';
    cancelBtn.style.border = 'none';
    cancelBtn.style.padding = '8px 16px';
    cancelBtn.style.borderRadius = '8px';
    cancelBtn.style.cursor = 'pointer';
    cancelBtn.style.fontWeight = '600';
    cancelBtn.onclick = () => { document.body.removeChild(modal); };
    buttonContainer.appendChild(cancelBtn);

    const okBtn = document.createElement('button');
    okBtn.textContent = 'ОК';
    okBtn.style.background = '#5d4a66';
    okBtn.style.color = 'white';
    okBtn.style.border = 'none';
    okBtn.style.padding = '8px 16px';
    okBtn.style.borderRadius = '8px';
    okBtn.style.cursor = 'pointer';
    okBtn.style.fontWeight = '600';
    okBtn.onclick = () => {
        const value = input.value;
        callback(value);
        document.body.removeChild(modal);
    };
    buttonContainer.appendChild(okBtn);

    dialog.appendChild(buttonContainer);
    modal.appendChild(dialog);
    document.body.appendChild(modal);

    modal.onclick = () => { document.body.removeChild(modal); };
    dialog.onclick = (e) => e.stopPropagation();
}
// Получение и обновление проектов из Android (если есть)
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

// Автоматический rerender/инициализация при загрузке страницы
document.addEventListener('DOMContentLoaded', function() {
    renderProjects();
    renderSchemes();
    showSection('projects');
});

const defaultPalette = {
  '--primary': '#ffbb8a',       // Цвет кнопок и выделения
  '--secondary': '#FEF7F9',     // Цвет секций и карточек
  '--accent': '#e07a5f',        // Цвет удаления и акцентов
  '--dark': '#5d4a66',          // Цвет текста
  '--light': '#fef7ff'          // Фон приложения
};
function applyPalette(palette) {
    for (const key in palette) {
        document.documentElement.style.setProperty(key, palette[key]);
    }
}
function loadPalette() {
    const palette = JSON.parse(localStorage.getItem('customPalette') || 'null');
    if (palette) applyPalette(palette);
}
function saveCustomPalette() {
  const palette = {
    '--primary': document.getElementById('colorPrimary').value,
    '--secondary': document.getElementById('colorSecondary').value,
    '--accent': document.getElementById('colorAccent').value,
    '--dark': document.getElementById('colorDark').value,
    '--light': document.getElementById('colorLight').value
  };
  localStorage.setItem('customPalette', JSON.stringify(palette));
  applyPalette(palette);
  fillPaletteInputs();
  alert('Палитра сохранена!');
}

function resetPalette() {
  localStorage.removeItem('customPalette');
  applyPalette(defaultPalette);
  fillPaletteInputs();
}
function fillPaletteInputs() {
  const palette = {...defaultPalette, ...JSON.parse(localStorage.getItem('customPalette') || '{}')};
  colorPrimary.value = palette['--primary'];
  colorSecondary.value = palette['--secondary'];
  colorAccent.value = palette['--accent'];
  colorDark.value = palette['--dark'];
  colorLight.value = palette['--light'];
  document.getElementById('previewPrimary').style.background = palette['--primary'];
  document.getElementById('previewSecondary').style.background = palette['--secondary'];
  document.getElementById('previewAccent').style.background = palette['--accent'];
  document.getElementById('previewDark').style.background = palette['--dark'];
  document.getElementById('previewLight').style.background = palette['--light'];
}

document.addEventListener('DOMContentLoaded', function() {
    loadPalette();
    fillPaletteInputs && fillPaletteInputs();
});

const defaultProjectIcon = '🧶';
function applyProjectIcon(icon) {
  document.querySelectorAll('.project-card .project-icon').forEach(el => {
    if (el && !el.closest('.add-project')) el.textContent = icon;
  });
}
function saveProjectIcon(icon) {
  localStorage.setItem('customProjectIcon', icon);
  applyProjectIcon(icon);
}
function loadProjectIcon() {
  const icon = localStorage.getItem('customProjectIcon') || defaultProjectIcon;
  applyProjectIcon(icon);
  document.querySelectorAll('.icon-choice').forEach(btn => {
    btn.classList.toggle('selected', btn.dataset.icon === icon);
  });
}
document.addEventListener('DOMContentLoaded', function() {
  loadProjectIcon();
  document.querySelectorAll('.icon-choice').forEach(btn => {
    btn.onclick = () => {
      saveProjectIcon(btn.dataset.icon);
      loadProjectIcon();
    };
  });
});

function showColorModal() {
  document.getElementById('colorModal').style.display = 'flex';
}
function closeColorModal() {
  document.getElementById('colorModal').style.display = 'none';
}





