import { check, sleep } from 'k6';
import http from 'k6/http';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api';

const PARTICIPANTS = Number(__ENV.PARTICIPANTS || 50);

export const options = {
  scenarios: {
    vote_flow: {
      executor: 'shared-iterations',
      vus: PARTICIPANTS,
      iterations: PARTICIPANTS,
      maxDuration: '2m',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'http_req_duration{type:room_read}': ['p(95)<700'],
    'http_req_duration{type:guest_enter}': ['p(95)<700'],
    'http_req_duration{type:vote_submit}': ['p(95)<500'],
    'http_req_duration{type:result_read}': ['p(95)<700'],
  },
};

export function setup() {
  const payload = JSON.stringify({
    password: 1234,
    location: {
      latitude: 37.5665,
      longitude: 126.9780,
    },
  });

  const res = http.post(`${BASE_URL}/rooms`, payload, {
    headers: {
      'Content-Type': 'application/json',
    },
    tags: {
      type: 'room_create',
    },
    timeout: '30s',
  });

  check(res, {
    'room create status is 201': (r) => r.status === 201,
  });

  if (res.status !== 201) {
    throw new Error(`방 생성 실패: status=${res.status}, body=${res.body}`);
  }

  const body = res.json();

  if (!body.roomId) {
    throw new Error(`roomId 없음: ${res.body}`);
  }

  if (!body.restaurantCards || body.restaurantCards.length === 0) {
    throw new Error(`음식점 후보 없음: ${res.body}`);
  }

  return {
    roomId: body.roomId,
    restaurantCards: body.restaurantCards,
  };
}

export default function (data) {
  const roomId = data.roomId;

  const roomRes = http.get(`${BASE_URL}/rooms/${roomId}`, {
    tags: {
      type: 'room_read',
    },
  });

  check(roomRes, {
    'room read status is 200': (r) => r.status === 200,
    'room has restaurant cards': (r) => {
      const body = r.json();
      return body.restaurantCards && body.restaurantCards.length > 0;
    },
  });

  const deviceId = `k6-device-${__VU}-${__ITER}-${Date.now()}-${Math.random()}`;

  const enterPayload = JSON.stringify({
    deviceId,
  });

  const enterRes = http.post(`${BASE_URL}/rooms/${roomId}/enter`, enterPayload, {
    headers: {
      'Content-Type': 'application/json',
    },
    tags: {
      type: 'guest_enter',
    },
  });

  check(enterRes, {
    'guest enter status is 200': (r) => r.status === 200,
    'sessionToken exists': (r) => !!r.json('sessionToken'),
  });

  if (enterRes.status !== 200) {
    return;
  }

  const sessionToken = enterRes.json('sessionToken');

  const cards = data.restaurantCards;
  const selectedPlaceIds = pickRandomPlaceIds(cards, 1);

  const votePayload = JSON.stringify({
    placeIds: selectedPlaceIds,
  });

  const voteRes = http.post(`${BASE_URL}/rooms/${roomId}/votes`, votePayload, {
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${sessionToken}`,
    },
    tags: {
      type: 'vote_submit',
    },
  });

  check(voteRes, {
    'vote submit status is 200': (r) => r.status === 200,
  });

  if (__VU % 5 === 0) {
    const resultRes = http.get(`${BASE_URL}/rooms/${roomId}/results`, {
      tags: {
        type: 'result_read',
      },
    });

    check(resultRes, {
      'result read status is 200': (r) => r.status === 200,
      'result has items': (r) => {
        const body = r.json();
        return body.results && body.results.length > 0;
      },
    });
  }

  sleep(1);
}

export function teardown(data) {
  const resultRes = http.get(`${BASE_URL}/rooms/${data.roomId}/results`, {
    tags: {
      type: 'result_read',
    },
  });

  if (resultRes.status === 200) {
    console.log(`final result: ${resultRes.body}`);
  } else {
    console.log(`result read failed: status=${resultRes.status}, body=${resultRes.body}`);
  }
}

function pickRandomPlaceIds(cards, count) {
  const copied = [...cards];
  const selected = [];

  while (selected.length < count && copied.length > 0) {
    const index = Math.floor(Math.random() * copied.length);
    const card = copied.splice(index, 1)[0];

    if (card && card.id) {
      selected.push(card.id);
    }
  }

  return selected;
}