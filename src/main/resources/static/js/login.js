const b64urlToBytes = async (b64url) => {
  const b64 = b64url.replaceAll("-", "+").replaceAll("_", "/"); //undo url-safe
  //ref: https://developer.mozilla.org/en-US/docs/Glossary/Base64#converting_arbitrary_binary_data
  const resp = await fetch(`data:application/octet-stream;base64,${b64}`);
  return new Uint8Array(await resp.arrayBuffer());
};

const bytesToB64Url = async (uint) => {
  const asFile = new File([uint], "", { type: "application/octet-stream" });
  const wrapper = new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.addEventListener("load", () => resolve(reader.result));
    reader.addEventListener("error", () => reject(reader.error));
    reader.readAsDataURL(asFile);
  });
  const b64 = (await wrapper).split(",")[1];
  return b64.replaceAll("=", "").replaceAll("+", "-").replaceAll("/", "_"); //url-safe
};

async function startAuth() {
  const initResponse = await fetch("/login/start");
  if (initResponse.status !== 200) {
    throw new Error(`Authentication failure: ${await initResponse.text()}`);
  }
  const credentialsRequest = await initResponse.json();
  const { challenge, allowCredentials, ...props } =
    credentialsRequest.publicKey;
  try {
    const credentials = await navigator.credentials.get({
      publicKey: {
        ...props,
        challenge: await b64urlToBytes(challenge),
        allowCredentials: allowCredentials?.map((c) => ({
          ...c,
          id: b64urlToBytes(c.id),
        })),
      },
    });
    const { authenticatorData, signature, clientDataJSON, userHandle } =
      credentials.response;
    const resultJson = {
      id: credentials.id,
      type: credentials.type,
      clientExtensionResults: credentials.getClientExtensionResults(),
      response: {
        authenticatorData: await bytesToB64Url(authenticatorData),
        signature: await bytesToB64Url(signature),
        clientDataJSON: await bytesToB64Url(clientDataJSON),
        userHandle: userHandle ? await bytesToB64Url(userHandle) : undefined,
      },
    };
    const finishResponse = await fetch("/login/finish", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(resultJson),
    });
    if (finishResponse.ok) {
      window.location.assign("/secure");
    } else {
      console.error(finishResponse);
    }
  } catch (authEx) {
    console.error(authEx);
  }
}

async function registerDevice(username) {
  const initData = new FormData();
  initData.set("name", username);
  const initResponse = await fetch("/register/start", {
    method: "POST",
    body: initData,
  });
  if (initResponse.status !== 200) {
    throw new Error(
      `Device registration failure: ${await initResponse.text()}`
    );
  }
  const credentialsRequest = await initResponse.json();
  const {
    challenge: pkChallenge,
    user,
    ...pkProps
  } = credentialsRequest.publicKey;
  const { id: userId, ...userProps } = user;
  const credentialsRequestBytes = {
    publicKey: {
      ...pkProps,
      challenge: await b64urlToBytes(pkChallenge),
      user: {
        ...userProps,
        id: await b64urlToBytes(userId),
      },
    },
  };
  try {
    const credentials = await navigator.credentials.create(
      credentialsRequestBytes
    );
    const { clientDataJSON, attestationObject } = credentials.response;
    const resultJson = {
      id: credentials.id,
      type: credentials.type,
      clientExtensionResults: credentials.getClientExtensionResults(),
      response: {
        clientDataJSON: await bytesToB64Url(clientDataJSON),
        attestationObject: await bytesToB64Url(attestationObject),
      },
    };
    console.debug(JSON.stringify(resultJson));
    const finishResponse = await fetch("/register/finish", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(resultJson),
    });
    if (finishResponse.ok) {
      window.location.assign("/");
    } else {
      console.error(finishResponse);
    }
  } catch (authEx) {
    console.error(authEx);
  }
}
