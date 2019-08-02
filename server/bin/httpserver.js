
const socket = require('socket.io')();
const users = [];
function addUser(id) {
    users.push(id);
}
function removeUserbyId(id) {
    var index = 0;
    while (index < users.length && id != users[index]) {
        index++;
    }
    users.splice(id, 1);
}
socket.on('connection', (client) => {
    console.log('connection success:' + client.id + ' users length:' + users.length);


    if (users.length > 2) {
        users.pop()
    }
    client.emit('id', client.id);
    if (users.length == 1) {
        //第二个用户连上来时候
        for (var index = 0; index < users.length; index++) {
            const element = users[index];
            if (element != client.id) {
                var otherClient = socket.sockets.connected[element];
                otherClient.emit('id', client.id);
                client.emit('id', element);
            }
        }
    }
    addUser(client.id);

    client.on('message', function (detail) {
        console.log('message-->' + detail.type + "<--->" + detail.to);
        var otherClient = socket.sockets.connected[detail.to];
        if (!otherClient) {
            client.emit('errorMsg', '请确认是否有在线用户~');
            return;
        }
        delete detail.to;
        detail.from = client.id;
        otherClient.emit('message', detail);
    });
    client.on('disconnect', function () {
        removeUserbyId(client.id);
        console.log(client.id + '  leave')
        client.emit('leave', client.id + ' leave');
    });
});
socket.listen(3000)
console.log('server is running~');
console.log('------------------');
