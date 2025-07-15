async function shorten() {
  const url = document.getElementById('longUrl').value;
  const resultDiv = document.getElementById('result');

  if (!url) {
    resultDiv.innerText = "Please enter a URL.";
    return;
  }

  try {
    const res = await fetch('/shorten', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      body: `url=${encodeURIComponent(url)}`
    });

    if (!res.ok) {
      resultDiv.innerText = "Error shortening URL.";
      return;
    }

    const shortUrl = await res.text();
    resultDiv.innerHTML = `<a href="${shortUrl}" target="_blank">${shortUrl}</a>`;
  } catch (err) {
    resultDiv.innerText = "Something went wrong.";
    console.error(err);
  }
}
