import http from "k6/http";
import {check, sleep} from "k6";

// export const options = {
//     scenarios: {
//         basic_load_test: {
//             executor: "ramping-vus",
//             stages: [
//                 {duration: "30s", target: 5},
//                 {duration: "1m", target: 20},
//                 {duration: "30s", target: 0},
//             ],
//         },
//     },
//     thresholds: {
//         http_req_failed: ["rate<0.01"],
//         http_req_duration: ["p(95)<500"],
//     },
// };

export const options = {
    vus: 1,
    duration: "10s"
}

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";

export default function () {
    const body = {
        password: 1234,
        location: {
            latitude: 37.500643,
            longitude: 127.036377
        }
    };

    const params = {
        headers: {
            "Content-Type": "application/json",
        },
    };

    const res = http.post(
        `${BASE_URL}/api/rooms`,
        JSON.stringify(body),
        params,
    );

    check(res, {
        "room create status is 201": (r) => r.status === 201,
    });

    sleep(1);
}
