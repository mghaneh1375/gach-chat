'use strict';

var usernamePage = document.querySelector('#username-page');
var chatPage = document.querySelector('#chat-page');
var usernameForm = document.querySelector('#usernameForm');
var messageForm = document.querySelector('#messageForm');
var messageInput = document.querySelector('#message');
var messageArea = document.querySelector('#messageArea');
var connectingElement = document.querySelector('.connecting');

var stompClient = null;
var username = null;
var subId;

var token = null;
var userId = null;
var recvId = null;
var prefix = "http://127.0.0.1:8088/";
// var prefix = "http://185.239.106.26:8088/";
var chatId = null;
var chats = [];
var PER_PAGE = 10;

var timer;

function isInCache(chatId) {

    for(var i = 0; i < chats.length; i++) {
        if(chats[i].chatId == chatId)
            return i;
    }

    return -1;
}

function isInCacheByRecv(recv) {

    for(var i = 0; i < chats.length; i++) {
        if(chats[i].receiverId == recv)
            return i;
    }

    return -1;
}

function isInCacheMsgs(msgs, chatId) {

    for(var i = 0; i < msgs.length; i++) {
        if(msgs[i].id == chatId)
            return i;
    }

    return -1;
}

function startChat() {
    subId = stompClient.subscribe('/chat/' + chatId, onMessageReceived, {'group-subscribe': true, 'token': token}).id;
}

function buildMsg(chat, chatId) {

    var html = "";

    if(chat.amISender)
        html += "<div class='msg sender'><p>" + chat.content + "</p><span>" + chat.createdAt + "</span></div>";
    else
        html += "<div class='msg receiver'><p>" + chat.content + "</p><span>" + chat.createdAt + "</span><span>" + chat.sender + "</span></div>";

    html += "<div class='clear'></div>";

    if(chatId != null) {
        var idx = isInCache(chatId);
        if (idx !== -1) {
            if (isInCacheMsgs(chats[idx].chats, chat.id) === -1) {
                chats[idx].chats.push(chat);
                chats[idx].totalMsgs++;
            }
        }
    }

    return html;
}

function left(subId) {

    if(stompClient) {
        var chatMessage = {
            chatId: chatId,
            token: token,
            type: 'LEAVE'
        };
        stompClient.send("/app/chat", {}, JSON.stringify(chatMessage));
        stompClient.unsubscribe(subId);
    }
}

function getSpecificChats(receiverId, mode) {

    if(chatId != null) {
        left(subId);
        chatId = null;
    }

    $(".contact").removeClass('selected-contact');
    $("#chat_" + receiverId).addClass('selected-contact');

    var idx = isInCacheByRecv(receiverId);
    if(idx !== -1) {
        chatId = chats[idx].chatId;
        sendHeart();
        renderThisChat(chats[idx], "FIFO", -1, -1);
        return;
    }

    $.ajax({
        type: 'get',
        url: prefix + 'api/chat/' + mode + "/" + receiverId + "/-1",
        headers: {
            "Accept": "application/json",
            "Authorization": "Bearer " + token
        },
        success: function (res) {

            if (res.status === "ok") {
                var chat = res.data;
                chat.receiverId = receiverId;
                chat.mode = mode;
                chats.push(chat);
                renderThisChat(chat, "FIFO", -1, -1);
                sendHeart();
                console.log(chats);
            }
        }

    });
}

function renderThisChat(chat, readMode, startIdx, limit) {

    chatId = chat.chatId;
    recvId = chat.receiverId;

    $("#new_msgs_chat_" + recvId).empty().append(0);

    var msgs = chat.chats;
    var html = "";
    var i;
    var allowStartSocket = false;

    if(startIdx === -1) {
        startIdx = Math.max(msgs.length - PER_PAGE, 0);
        allowStartSocket = true;
    }

    if(readMode === "FIFO") {

        limit = (limit === -1) ? Math.min(startIdx + PER_PAGE, msgs.length)
            : Math.min(startIdx + limit, startIdx + PER_PAGE);

        for (i = startIdx; i < limit; i++)
            html += buildMsg(msgs[i], null);
    }

    if(allowStartSocket) {
        $("#messageArea").empty().append(html)
            .scrollTop($("#messageArea")[0].scrollHeight);

        startChat();
    }
    else
        $("#messageArea").prepend(html)
            .scrollTop($("#messageArea").scrollTop() + 60 * limit - startIdx + 1);
}

function getChats() {

    $.ajax({
        type: 'get',
        url: prefix + 'api/chats',
        headers: {
            "Accept": "application/json",
            "Authorization": "Bearer " + token
        },
        success: function (res) {

            if (res.status === "ok") {
                usernamePage.classList.add('hidden');
                chatPage.classList.remove('hidden');

                var html = "";
                for(var i = 0; i < res.chats.length; i++)
                    html += buildChat(res.chats[i]);

                $("#contact-container").append(html);

            }
        }

    });
}

function buildChat(chat) {

    var html = '<div id="chat_' + chat.receiverId + '" class="contact" onclick="getSpecificChats(\'' + chat.receiverId + '\', \'' + chat.mode + '\')">';
    html += '<div class="avatar">' + chat.receiverName[0] +'</div>';
    html += '<div>' + chat.receiverName + '</div>';
    html += '<p id="new_msgs_chat_' + chat.receiverId + '">' + chat.newMsgs + '</p>';
    html += '</div>';

    return html;
}

function heartBeatHandler() {

    stompClient.subscribe("/chat/" + userId, onNewMessageReceived, {'self-subscribe': true, 'token': token});
    connectingElement.classList.add('hidden');

    sendHeart();

    timer = setInterval(function () {
        sendHeart();
    }, 5000);

    getChats();
}

function sendHeart() {

    if(stompClient) {

        var msg = {
            token: token,
            type: 'heart'
        };

        if (chatId != null)
            msg.chatId = chatId;

        stompClient.send("/app/chat", {}, JSON.stringify(msg));
    }

}

function login(username) {

    $.ajax({
        type: 'post',
        url: prefix + 'api/user/signIn',
        headers: {
            "Content-Type": "application/json",
            "Accept": "application/json"
        },
        data: JSON.stringify({
            username: username,
            password: "1"
        }),
        success: function (res) {

            if(res.status === "ok") {
                token = res.token;
                window.localStorage.token = token;
                userId = res.id;

                var authToken = 'R3YKZFKBVi';

                document.cookie = 'X-Authorization=' + authToken + '; path=/';

                var socket = new SockJS(prefix + "ws", ["token", token]);
                stompClient = Stomp.over(socket);

                stompClient.connect({}, heartBeatHandler, onError);
            }

        }
    });

}

function isAuth(token) {

    $.ajax({
        type: 'post',
        url: prefix + 'api/user/isAuth',
        headers: {
            "Authorization": "Bearer " + token,
            "Accept": "application/json"
        },
        success: function (res) {

            if(res.status === "ok") {
                userId = res.id;
                $("#user_name").append(res.name);

                var socket = new SockJS(prefix + "ws");
                stompClient = Stomp.over(socket);

                stompClient.connect({"token": token}, heartBeatHandler, onError);
            }

        }
    });

}

function connect(event) {

    username = document.querySelector('#name').value.trim();

    if(username) {
        login(username);
    }

    event.preventDefault();
}

function onError(error) {
    clearInterval(timer);
    alert(error);
}


function sendMessage(event) {
    var messageContent = messageInput.value.trim();
    if(messageContent && stompClient) {
        var chatMessage = {
            chatId: chatId,
            token: token,
            content: messageInput.value,
            type: 'send'
        };
        stompClient.send("/app/chat", {}, JSON.stringify(chatMessage));
        messageInput.value = '';
    }
    event.preventDefault();

    var html = $("#chat_" + recvId).clone();
    $("#chat_" + recvId).remove();

    $("#contact-container").prepend(html).scrollTop(0)
}


function onMessageReceived(payload) {

    var message = JSON.parse(payload.body);
    console.log("onMessageReceived");
    console.log(message);

    var msg = {
        "content": message.content,
        "amISender": message.senderId == userId,
        "createdAt": message.timestamp,
        "id": message.id
    }

    if(!msg.amISender)
        msg.sender = message.sender;

    $("#messageArea").append(buildMsg(msg, message.chatId));
    messageArea.scrollTop = messageArea.scrollHeight;
}

function onNewMessageReceived(payload) {

    var message = JSON.parse(payload.body);

    console.log("onNewMessageReceived");
    console.log(message);

    var html;
    var elem = $("#new_msgs_chat_" + message.chatId);

    var idx = isInCacheByRecv(message.chatId);
    if(idx !== -1) {

        var chatMessage = message.chatMessage;
        var msg = {
            "content": chatMessage.content,
            "amISender": chatMessage.senderId == userId,
            "createdAt": chatMessage.timestamp,
            "id": chatMessage.id
        }

        if(!msg.amISender)
            msg.sender = chatMessage.sender;

        chats[idx].chats.push(msg);
    }

    if(elem.length > 0) {
        elem.empty().append(message.count);
        html = $("#chat_" + message.chatId).clone();
        $("#chat_" + message.chatId).remove();
    }
    else if(message.mode === "peer") {

        html = buildChat({
            "receiverId": message.chatId,
            "newMsgs": message.count,
            "receiverName": message.senderName,
            "mode": message.mode
        });

    }

    $("#contact-container").prepend(html);
}

usernameForm.addEventListener('submit', connect, true)
messageForm.addEventListener('submit', sendMessage, true)

function getAnotherPage() {

    var idx = isInCache(chatId);
    if(idx === -1)
        return;

    var currChilds = $("#messageArea .msg").length;
    if(currChilds === chats[idx].totalMsgs)
        return;

    if(currChilds < chats[idx].chats.length)
        renderThisChat(chats[idx], "FIFO", Math.max(0, chats[idx].chats.length - currChilds - PER_PAGE), chats[idx].chats.length - currChilds);
    else {

        $.ajax({
            type: 'get',
            url: prefix + 'api/chat/' + chats[idx].mode + "/" + chats[idx].receiverId + "/" + chats[idx].chats[0].createdAt,
            headers: {
                "Accept": "application/json",
                "Authorization": "Bearer " + token
            },
            success: function (res) {

                if (res.status === "ok") {

                    var chat = res.data;

                    for(var i = chat.chats.length - 1; i >= 0; i--)
                        chats[idx].chats.unshift(chat.chats[i]);

                    renderThisChat(chats[idx], "FIFO", 0, chat.chats.length);
                }
            }

        });
    }
}

$(document).ready(function () {

    token = window.localStorage.token;

    if(token === undefined || token == null || token === "null") {
        $("#chat-page").addClass("hidden");
        $("#username-page").removeClass("hidden");
    }
    else
        isAuth(token);

    $("#messageArea").scroll(function() {
        var pos = $('#messageArea').scrollTop();
        if (pos === 0)
            getAnotherPage();
    });

    // 9122835126 : teacher
    // 2248089   612b60bb12cfce6c2cb1535a
    // 9127222752 61349ea7384cb720593b757b
    // 9357358999


    // 61324bb10847d93d5125df6f    9122630845
    // 613237950847d93d5125df39
});