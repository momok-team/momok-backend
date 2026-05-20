import { check, sleep } from 'k6';
import http from 'k6/http';
import { Trend } from 'k6/metrics';
import ws from 'k6/ws';

  const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api';
  const WS_URL = __ENV.WS_URL || 'ws://localhost:8080/api/ws';

  export const presenceCount = new Trend('presence_count');

  export const options = {
    scenarios: {
      websocket_presence: {
        executor: 'constant-vus',
        vus: Number(__ENV.VUS || 20),
        duration: __ENV.DURATION || '40s',
      },
    },
  };

  export function setup() {
    const createRes = http.post(`${BASE_URL}/rooms`, JSON.stringify({
      password: 1234,
      location: {
        latitude: 37.5665,
        longitude: 126.9780,
      },
    }), {
      headers: { 'Content-Type': 'application/json' },
      timeout: '30s',
    });

    if (createRes.status !== 201) {
      throw new Error(`방 생성 실패: ${createRes.status} ${createRes.body}`);
    }

    return createRes.json();
  }

  export default function (room) {
    const roomId = room.roomId;

    const enterRes = http.post(`${BASE_URL}/rooms/${roomId}/enter`, JSON.stringify({
      deviceId: `ws-device-${__VU}-${__ITER}`,
    }), {
      headers: { 'Content-Type': 'application/json' },
    });

    if (enterRes.status !== 200) {
      return;
    }

    const sessionToken = enterRes.json('sessionToken');

    const res = ws.connect(WS_URL, {}, function (socket) {
      socket.on('open', function () {
        socket.send('CONNECT\naccept-version:1.2\nheart-beat:10000,10000\n\n\0');
      });

      socket.on('message', function (message) {
        if (message.startsWith('CONNECTED')) {
          socket.send(
            `SUBSCRIBE\nid:sub-${__VU}\ndestination:/topic/rooms/${roomId}/presence\n\n\0`
          );

          socket.setInterval(function () {
            socket.send(
              `SEND\ndestination:/app/rooms/${roomId}/ping\ncontent-type:application/json\n\n${JSON.stringify({ sessionToken })}\0`
            );
          }, 5000);
        }

        if (message.startsWith('MESSAGE') && message.includes('activeUserCount')) {
          const jsonStart = message.indexOf('{');
          const jsonEnd = message.lastIndexOf('}');

          if (jsonStart >= 0 && jsonEnd > jsonStart) {
            const body = JSON.parse(message.substring(jsonStart, jsonEnd + 1));
            presenceCount.add(body.activeUserCount);

            if (__VU === 1) {
              console.log(`activeUserCount=${body.activeUserCount}`);
            }
          }
        }
      });

      socket.setTimeout(function () {
        socket.close();
      }, 30000);
    });

    check(res, {
      'websocket connected': (r) => r && r.status === 101,
    });

    sleep(1);
  }