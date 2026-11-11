const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const { initializeApp } = require("firebase-admin/app");
const { getDatabase } = require("firebase-admin/database");
const crypto = require("crypto");

initializeApp();
const OWNER_BOOTSTRAP_KEY = defineSecret("OWNER_BOOTSTRAP_KEY");
const db = () => getDatabase();

function requireAuth(request) {
  if (!request.auth) throw new HttpsError("unauthenticated", "Login session required.");
  return request.auth.uid;
}
function cleanDeviceId(value) {
  if (typeof value !== "string" || value.length < 8 || value.length > 128)
    throw new HttpsError("invalid-argument", "Invalid device identifier.");
  return value;
}
function makeKey() {
  const alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  const bytes = crypto.randomBytes(13);
  const chars = [...bytes].map(b => alphabet[b % alphabet.length]);
  return chars.slice(0,4).join("") + "-" +
         chars.slice(4,9).join("") + "-" +
         chars.slice(9,13).join("");
}

exports.redeemKey = onCall({ secrets: [OWNER_BOOTSTRAP_KEY], enforceAppCheck: false }, async (request) => {
  const uid = requireAuth(request);
  const key = String(request.data?.key || "").trim().toUpperCase();
  const deviceId = cleanDeviceId(request.data?.deviceId);
  if (!/^[A-Z0-9]{4}-[A-Z0-9]{5}-[A-Z0-9]{4}$/.test(key))
    throw new HttpsError("invalid-argument", "Format key harus XXXX-XXXXX-XXXX.");

  const keyRef = db().ref("keys/" + key);
  if (key === OWNER_BOOTSTRAP_KEY.value()) {
    await keyRef.transaction(current => current || {
      role: "OWNER", active: true, used: false, createdAt: Date.now(), source: "bootstrap"
    });
  }

  const result = await keyRef.transaction(current => {
    if (!current || current.active !== true) return;
    if (current.boundUid) {
      if (current.boundUid === uid && current.deviceId === deviceId) return current;
      return;
    }
    current.used = true;
    current.boundUid = uid;
    current.deviceId = deviceId;
    current.boundAt = Date.now();
    return current;
  });

  const record = result.snapshot.val();
  if (!result.committed || !record || record.boundUid !== uid || record.deviceId !== deviceId)
    throw new HttpsError("permission-denied", "Key tidak valid, nonaktif, atau sudah terikat ke perangkat lain.");

  const profile = {
    role: record.role,
    key,
    deviceId,
    active: true,
    createdAt: Date.now()
  };
  await db().ref("users/" + uid).set(profile);
  return { role: profile.role, active: true };
});

exports.getProfile = onCall(async (request) => {
  const uid = requireAuth(request);
  const deviceId = cleanDeviceId(request.data?.deviceId);
  const snap = await db().ref("users/" + uid).get();
  const p = snap.val();
  if (!p || p.active !== true || p.deviceId !== deviceId)
    return { active: false };
  return { active: true, role: p.role, key: p.key };
});

exports.generateKey = onCall(async (request) => {
  const uid = requireAuth(request);
  const profileSnap = await db().ref("users/" + uid).get();
  const profile = profileSnap.val();
  if (!profile || profile.active !== true || profile.role !== "OWNER")
    throw new HttpsError("permission-denied", "OWNER role required.");

  const requestedRole = String(request.data?.role || "MEMBER").toUpperCase();
  if (!["MEMBER", "OWNER"].includes(requestedRole))
    throw new HttpsError("invalid-argument", "Invalid role.");

  for (let i = 0; i < 8; i++) {
    const key = makeKey();
    const ref = db().ref("keys/" + key);
    const createdAt = Date.now();
    const result = await ref.transaction(current => {
      if (current) return;
      return {
        role: requestedRole,
        active: true,
        used: false,
        createdBy: uid,
        createdAt
      };
    });
    if (result.committed && result.snapshot.val()?.createdBy === uid)
      return { key, role: requestedRole };
  }
  throw new HttpsError("resource-exhausted", "Could not allocate a unique key. Try again.");
});
    
