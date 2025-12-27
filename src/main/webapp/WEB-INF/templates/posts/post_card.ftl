<div class="post-card">
    <h3>${post.title}</h3>
    <div>Автор: ${post.userId}</div>
    <p>${post.body}</p>

    <div class="post-actions">
        <button class="btn-edit" onclick="loadPost(${post.id})">Редактировать</button>
        <button class="btn-delete" onclick="deletePost(${post.id})">Удалить</button>
    </div>
</div>