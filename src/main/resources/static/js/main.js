function setErrorToast(msgMaybe) {
  const toast = document.getElementById("error-toast");
  toast.innerText = msgMaybe ?? "";
  toast.style.display = msgMaybe ? "block" : "none";
}

document.body.addEventListener("htmx:afterRequest", function (evt) {
  if (evt?.detail?.successful) {
    setErrorToast(null);
  } else {
    const msg =
      evt?.detail?.xhr?.status == 409
        ? "name already in use"
        : evt?.detail?.xhr?.statusText ?? "request failed";
    setErrorToast("Error: " + msg);
  }
});
