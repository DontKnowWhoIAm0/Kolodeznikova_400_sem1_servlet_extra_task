<!DOCTYPE html>
<html>
    <head>
        <title>Posts</title>
        <script src="static/js/posts.js"></script>
    </head>

    <body>
        <div id="postsContainer">
            <#list posts as post>
                <#include "post_card.ftl">
            </#list>
        </div>

        <div id="pagination">
            <#list 1..totalPages as p>
                <button class="page-btn <#if p == currentPage>active</#if>" onclick="loadPosts(${p}, '${order}')">${p}</button>
            </#list>
        </div>

        <div id="createModal" class="modal" style="display:none;">
            <div class="modal-content">
                <h2>Создать пост</h2>
                <input id="createTitle" placeholder="Заголовок">
                <textarea id="createBody" placeholder="Текст"></textarea>
                <input id="createUserId" type="number" placeholder="User ID">
                <button onclick="createPost()">Создать</button>
                <button onclick="hideModal('createModal')">Отмена</button>
            </div>
        </div>

        <div id="editModal" class="modal" style="display:none;">
            <div class="modal-content">
                <h2>Редактировать пост</h2>
                <input id="editPostId" type="hidden">
                <input id="editTitle" placeholder="Заголовок">
                <textarea id="editBody" placeholder="Текст"></textarea>
                <input id="editUserId" type="number" placeholder="User ID">
                <button onclick="updatePost()">Сохранить</button>
                <button onclick="hideModal('editModal')">Отмена</button>
            </div>
        </div>
    </body>
</html>
