async function register() {
  const username = document.getElementById("regUser").value;
  const password = document.getElementById("regPass").value;
  const msg = document.getElementById("regMsg");

  msg.innerText = "";

  if (!username || !password) {
    msg.innerText = "Both fields required.";
    return;
  }

  const response = await fetch("/register", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `username=${username}&password=${password}`
  });

  const text = await response.text();
  msg.innerText = text;
}

async function login() {
  const username = document.getElementById("loginUser").value;
  const password = document.getElementById("loginPass").value;
  const msg = document.getElementById("loginMsg");

  msg.innerText = "";

  if (!username || !password) {
    msg.innerText = "Both fields required.";
    return;
  }

  const response = await fetch("/login", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `username=${username}&password=${password}`
  });

  const text = await response.text();
  msg.innerText = text;
}

async function shorten() {
  const url = document.getElementById("url").value;
  const username = document.getElementById("username").value;
  const customCode = document.getElementById("customCode").value;

  const resultBox = document.getElementById("result");
  const errorBox = document.getElementById("error");
  resultBox.innerText = "";
  errorBox.innerText = "";

  if (!url) {
    errorBox.innerText = "URL is required.";
    return;
  }

  const params = new URLSearchParams();
  params.append("url", url);
  if (username) params.append("username", username);
  if (customCode) params.append("customCode", customCode);

  const response = await fetch("/shorten", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: params.toString()
  });

  const text = await response.text();

  if (response.ok) {
    resultBox.innerHTML = `Shortened URL: <a href="${text}" target="_blank">${text}</a>`;
  } else {
    errorBox.innerText = text;
  }
}
