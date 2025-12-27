const API_BASE = '/api/posts';

function showModal(modalId) {
    document.getElementById(modalId).style.display = 'block';
    document.body.style.overflow = 'hidden';
}

function hideModal(modalId) {
    document.getElementById(modalId).style.display = 'none';
    document.body.style.overflow = 'auto';
}

async function loadPosts(page = 1, order = 'ASC') {
    try {
        const response = await fetch(`${API_BASE}?page=${page}&order=${order}`);
        const data = await response.json();
        drawPosts(data.posts, data.currentPage, data.totalPages, order);
    } catch (error) {
        console.error('Ошибка загрузки постов:', error);
    }
}


async function loadPost(id) {
    try {
        const response = await fetch(`${API_BASE}/${id}`);
        if (response.ok) {
            const post = await response.json();
            document.getElementById('editPostId').value = post.id;
            document.getElementById('editTitle').value = post.title;
            document.getElementById('editBody').value = post.body;
            document.getElementById('editUserId').value = post.userId;
            showModal('editModal');
        }
    } catch (error) {
        console.error('Ошибка загрузки поста:', error);
    }
}


async function createPost() {
    const title = document.getElementById('createTitle').value;
    const body = document.getElementById('createBody').value;
    const userId = document.getElementById('createUserId').value;

    if (!title || !body || !userId) {
        alert('Заполните все поля!');
        return;
    }

    try {
        const postData = { title, body, userId: parseInt(userId) };
        const response = await fetch(API_BASE, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(postData)
        });

        if (response.ok) {
            hideModal('createModal');
            document.getElementById('createTitle').value = '';
            document.getElementById('createBody').value = '';
            document.getElementById('createUserId').value = '';
            loadPosts(1, 'ASC');
        } else {
            alert('Ошибка создания поста');
        }
    } catch (error) {
        console.error('Ошибка создания поста:', error);
    }
}


async function updatePost() {
    const id = document.getElementById('editPostId').value;
    const title = document.getElementById('editTitle').value;
    const body = document.getElementById('editBody').value;
    const userId = document.getElementById('editUserId').value;

    if (!title || !body || !userId) {
        alert('Заполните все поля!');
        return;
    }

    try {
        const postData = {id: parseInt(id), title, body, userId: parseInt(userId) };

        const response = await fetch(`${API_BASE}/${id}`, {
            method: 'PUT',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(postData)
        });

        if (response.ok) {
            hideModal('editModal');
            loadPosts();
        } else {
            alert('Ошибка обновления поста');
        }
    } catch (error) {
        console.error('Ошибка обновления поста:', error);
    }
}

async function deletePost(id) {
    if (!confirm('Вы уверены, что хотите удалить этот пост?')) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE}?id=${id}`, {method: 'DELETE'});

        if (response.ok) {
            loadPosts();
        } else {
            alert('Ошибка удаления поста');
        }
    } catch (error) {
        console.error('Ошибка удаления поста:', error);
    }
}

function drawPosts(posts, currentPage, totalPages, order) {
    const container = document.getElementById('postsContainer');
    container.innerHTML = posts.map(post => `
        <div class="post-card">
            <h3>${post.title}</h3>
            <div>Автор: ${post.userId}</div>
            <p>${post.body}</p>
            <div class="post-actions">
                <button class="btn-edit" onclick="loadPost(${post.id})">Редактировать</button>
                <button class="btn-delete" onclick="deletePost(${post.id})">Удалить</button>
            </div>
        </div>
    `).join('');

    drawPagination(currentPage, totalPages, order);
}

function drawPagination(currentPage, totalPages, order) {
    const pagination = document.getElementById('pagination');
    let html = '';

    for (let p = 1; p <= totalPages; p++) {
        const activeClass = p === currentPage ? 'active' : '';
        html += `<button class="page-btn ${activeClass}" onclick="loadPosts(${p}, '${order}')">${p}</button>`;
    }

    pagination.innerHTML = html;
}


document.addEventListener('DOMContentLoaded', function() {
    loadPosts(1, 'ASC');
    document.addEventListener('click', function(e) {
        if (e.target.classList.contains('modal')) {
            hideModal('createModal');
            hideModal('editModal');
        }
    });
});