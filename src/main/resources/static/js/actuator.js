fetchJson("/actuator/health/liveness")
    .then(setStatusElement("live-state", "LIVE"))
    .catch(handleErrors);

fetchJson("/actuator/health/readiness")
    .then(setStatusElement("ready-state", "READY", "NOT READY"))
    .catch(handleErrors);

fetchJson("/actuator/health")
    .then(setStatusElement("healthy-state", "HEALTHY", "UNHEALTHY"))
    .catch(handleErrors);

fetchJson("/actuator/health")
    .then(function (data) {
        return Object.entries(data["components"])
            .filter(([_, v]) => v.hasOwnProperty("status"))
            .map(([k, v]) => [k, v["status"]])
            .map(([k, v]) => [k, `<a href="actuator/health/${k}">${v}</a>`])
            .map(entryToTitleCase);
    })
    .then(entriesToList)
    .then(insertHtmlBeforeEnd("actuator-health"))
    .catch(handleErrors);

function populateMetrics(elementId) {
    if (!isElementPopulated(elementId)) {
        fetchJsonOrThrow("/actuator/metrics")
            .then(function (data) {
                return data["names"]
                    .map(m => [m, `/actuator/metrics/${m}`]);
            })
            .then(entriesToHrefList)
            .then(insertHtmlBeforeEnd(elementId))
            .catch(handleErrors);
    }
}

function populateInfo(elementId) {
    if (!isElementPopulated(elementId)) {
        fetchJsonOrThrow("/actuator/info")
            .then(entriesToNestedList)
            .then(insertHtmlBeforeEnd(elementId))
            .catch(handleErrors);
    }
}

function populateEndpoints(elementId) {
    if (!isElementPopulated(elementId)) {
        fetchJsonOrThrow("/actuator")
            .then(function (data) {
                return Object.entries(data["_links"])
                    .filter(([_, v]) => v.hasOwnProperty("href"))
                    .map(([k, v]) => [k === "self" ? "actuator" : k, v])
                    .map(([k, v]) => [k, v["href"]])
                    .filter(([_, v]) => !v.includes("{"))
                    .map(entryToTitleCase);
            })
            .then(entriesToHrefList)
            .then(insertHtmlBeforeEnd(elementId))
            .catch(handleErrors);
    }
}

function setStatusElement(elementId, upText = "UP", downText = "DOWN", unknownText = "UNKNOWN") {
    return function (data) {
        let element = document.getElementById(elementId);
        element.classList.remove("up", "down", "unknown");

        if (data.hasOwnProperty("status")) {
            if (data["status"] === "UP") {
                element.classList.add("up");
                element.innerText = upText;
            } else if (data["status"] === "DOWN") {
                element.classList.add("down");
                element.innerText = downText;
            } else {
                element.classList.add("unknown");
                element.innerText = data["status"];
            }
        } else {
            element.classList.add("unknown");
            element.innerText = unknownText;
        }
    };
}

function entriesToNestedList(data) {
    return entriesToList(Object.entries(data)
        .map(([k, v]) => [k, typeof v === "object" && Object.keys(v).length > 0 ? entriesToNestedList(v) : v])
        .map(entryToTitleCase)
    );
}

function entriesToHrefList(entries) {
    return entriesToList(entries, ([k, v]) => `<a href="${v}">${k}</a>`);
}

function entriesToList(entries, keyValueMapper = ([k, v]) => `${k}: ${v}`) {
    let html = ["<ul>"];
    entries
        .sort()
        .map(keyValueMapper)
        .map(a => `<li>${a}</li>`)
        .forEach(a => html.push(a));
    html.push("</ul>")
    return html.join("");
}

function insertHtmlBeforeEnd(elementId) {
    return function (html) {
        let element = document.getElementById(elementId);
        element.insertAdjacentHTML("beforeend", html)
        element.setAttribute("populated", "true");
    }
}

function isElementPopulated(elementId) {
    return document.getElementById(elementId).getAttribute("populated") !== null;
}

function entryToTitleCase([k, v]) {
    let title = k.replace(/([A-Z]+)/g, " $1")
        .split(/[ ._-]/)
        .map(w => w.charAt(0).toUpperCase() + w.substring(1))
        .join(" ");
    return [title.length < 3 || !title.match(/[aeiou ]/i) ? title.toUpperCase() : title, v];
}

function fetchJson(input) {
    return fetch(input)
        .then(function (response) {
            if (response === null) {
                throw Error("Response was null");
            } else if (response.status === 404) {
                throw Error("Endpoint not enabled");
            }
            return response.json();
        });
}

function fetchJsonOrThrow(input) {
    return fetch(input)
        .then(function (response) {
            if (!response.ok) {
                throw Error(response.statusText);
            }
            return response.json();
        });
}

function handleErrors(error) {
    return console.log(error);
}
