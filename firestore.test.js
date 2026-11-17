// @ts-nocheck
/* eslint-env node, commonjs */
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function pastDate() {
  return new Date(Date.now() - 2000);
}

function validAddon(uid) {
  const now = pastDate();
  return {
    userId: uid,
    addonKey: "cinemeta_v3",
    manifestUrl: "https://v3-cinemeta.strem.io/manifest.json",
    name: "Cinemeta",
    version: "3.0.1",
    description: "Official movie and series metadata catalog",
    types: ["movie", "series"],
    resources: ["catalog", "meta"],
    enabled: true,
    createdAt: now,
    updatedAt: now,
  };
}

function validLibraryItem(uid) {
  const now = pastDate();
  return {
    userId: uid,
    mediaKey: "tt0133093",
    title: "The Matrix",
    mediaType: "movie",
    posterUrl: "https://images.metahub.space/poster/medium/tt0133093/img",
    backdropUrl: "",
    overview: "A hacker discovers the nature of reality.",
    year: "1999",
    imdbRating: "8.7",
    streamUrl: "",
    sourceLabel: "Cinemeta",
    isFavorite: true,
    watchProgressMs: 120000,
    durationMs: 8160000,
    lastWatchedAtMs: 1700000000000,
    createdAt: now,
    updatedAt: now,
  };
}

function validPreferences(uid) {
  const now = pastDate();
  return {
    userId: uid,
    preferredEngine: "EXOPLAYER",
    hardwareDecoding: true,
    subtitleFontSizeSp: 18,
    subtitleColorHex: "#FFFFFF",
    subtitleBgOpacity: 0.65,
    subtitleDelayMs: 0,
    preferredSubtitleLang: "en",
    preferredAudioLang: "en",
    accentPreset: "TUNGSTEN_GOLD",
    compactGrid: false,
    autoFetchLocalMetadata: true,
    downloadWifiOnly: true,
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user: cannot read or write addons, library, or preferences", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  await assertFails(db.collection("users").doc(ALICE_UID).collection("addons").get());
  await assertFails(db.collection("users").doc(ALICE_UID).collection("library").get());
  await assertFails(db.collection("users").doc(ALICE_UID).collection("preferences").doc("default").get());
  await assertFails(db.collection("users").doc(ALICE_UID).collection("addons").doc("cinemeta_v3").set(validAddon(ALICE_UID)));
});

test("Authenticated user: can create, update, list, and delete own addon", async () => {
  const db = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = db.collection("users").doc(ALICE_UID).collection("addons").doc("cinemeta_v3");
  await assertSucceeds(ref.set(validAddon(ALICE_UID)));
  await assertSucceeds(db.collection("users").doc(ALICE_UID).collection("addons").get());
  await assertSucceeds(ref.update({ enabled: false, updatedAt: new Date(Date.now() - 1000) }));
  await assertSucceeds(ref.delete());
});

test("Cross-user isolation: Bob cannot read or write Alice's addons or library", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("library").doc("tt0133093").set(validLibraryItem(ALICE_UID))
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("library").doc("tt0133093").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("library").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("library").doc("tt0133093").set(validLibraryItem(BOB_UID))
  );
});

test("Shadow update & schema validation: rejects ghost fields and invalid enums", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const addonPayload = { ...validAddon(ALICE_UID), isAdmin: true };
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("addons").doc("ghost_test").set(addonPayload)
  );

  const badPrefs = { ...validPreferences(ALICE_UID), preferredEngine: "VLC_FAKE" };
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("preferences").doc("default").set(badPrefs)
  );
});

test("Preferences CRUD: valid user preferences succeed and block immutable field tampering", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const prefRef = aliceDb.collection("users").doc(ALICE_UID).collection("preferences").doc("default");
  await assertSucceeds(prefRef.set(validPreferences(ALICE_UID)));
  await assertSucceeds(prefRef.update({ preferredEngine: "LIBMPV", subtitleFontSizeSp: 22, updatedAt: new Date(Date.now() - 1000) }));
  await assertFails(prefRef.update({ userId: BOB_UID, updatedAt: new Date(Date.now() - 1000) }));
});
