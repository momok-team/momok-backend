import { check } from 'k6';
import http from 'k6/http';
import { Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api';
const DUPLICATE_REQUESTS = Number(__ENV.DUPLICATE_REQUESTS || 20);

export const duplicateSuccess = new Counter('duplicate_vote_success');
export const duplicateRejected = new Counter('duplicate_vote_rejected');

export const options = {
  scenarios: {
    duplicate_vote: {
      executor: 'shared-iterations',
      vus: DUPLICATE_REQUESTS,
      iterations: DUPLICATE_REQUESTS,
      maxDuration: '30s',
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

  const room = createRes.json();

  const enterRes = http.post(`${BASE_URL}/rooms/${room.roomId}/enter`, JSON.stringify({
    deviceId: 'duplicate-test-device',
  }), {
    headers: { 'Content-Type': 'application/json' },
  });

  if (enterRes.status !== 200) {
    throw new Error(`입장 실패: ${enterRes.status} ${enterRes.body}`);
  }

  return {
    roomId: room.roomId,
    sessionToken: enterRes.json('sessionToken'),
    placeId: room.restaurantCards[0].id,
  };
}

export default function (data) {
  const res = http.post(`${BASE_URL}/rooms/${data.roomId}/votes`, JSON.stringify({
    placeIds: [data.placeId],
  }), {
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${data.sessionToken}`,
    },
    tags: { type: 'duplicate_vote' },
  });

  if (res.status === 200) {
    duplicateSuccess.add(1);
  } else {
    duplicateRejected.add(1);
  }

  check(res, {
    'vote is success or rejected': (r) => r.status === 200 || r.status >= 400,
  });
}