// Receive-only SMTP stub: just enough of the protocol to confirm the app really sends.
const net = require('net');
const fs = require('fs');
const out = process.argv[2];
const server = net.createServer(sock => {
  let inData = false, buf = '', message = '';
  sock.write('220 stub ESMTP\r\n');
  sock.on('data', chunk => {
    buf += chunk.toString('utf8');
    while (true) {
      if (inData) {
        const end = buf.indexOf('\r\n.\r\n');
        if (end < 0) { message += buf; buf = ''; return; }
        message += buf.slice(0, end);
        buf = buf.slice(end + 5);
        inData = false;
        fs.writeFileSync(out, message);
        sock.write('250 OK queued\r\n');
        continue;
      }
      const nl = buf.indexOf('\r\n');
      if (nl < 0) return;
      const line = buf.slice(0, nl); buf = buf.slice(nl + 2);
      const cmd = line.split(' ')[0].toUpperCase();
      if (cmd === 'EHLO' || cmd === 'HELO') sock.write('250-stub\r\n250 SIZE 10485760\r\n');
      else if (cmd === 'MAIL' || cmd === 'RCPT') sock.write('250 OK\r\n');
      else if (cmd === 'DATA') { inData = true; sock.write('354 End data with <CR><LF>.<CR><LF>\r\n'); }
      else if (cmd === 'QUIT') { sock.write('221 Bye\r\n'); sock.end(); }
      else if (cmd === 'RSET' || cmd === 'NOOP') sock.write('250 OK\r\n');
      else sock.write('502 not implemented\r\n');
    }
  });
  sock.on('error', () => {});
});
server.listen(2525, '127.0.0.1', () => console.log('smtp stub on 2525'));
