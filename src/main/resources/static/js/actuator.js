fetch("/actuator")
    .then(throwOrJson)
    .then(function (data) {
        return Object.entries(data["_links"])
            .filter(([_, v]) => v.hasOwnProperty("href"))
            .map(([k, v]) => [k === "self" ? "actuator" : k, v])
            .map(([k, v]) => [k, v["href"]])
            .filter(([_, v]) => !v.includes("{"))
            .map(toTitleCase);
    })
    .then(entriesToHrefList)
    .then(function (html) {
        insertHtmlBeforeEnd("actuator-links", html);
    })
    .catch(handleErrors);

fetch("/actuator/metrics")
    .then(throwOrJson)
    .then(function (data) {
        return data["names"]
            .map(m => [m, `/actuator/metrics/${m}`]);
    })
    .then(entriesToHrefList)
    .then(function (html) {
        insertHtmlBeforeEnd("actuator-metrics", html);
    })
    .catch(handleErrors);

fetch("/actuator/health")
    .then(throwOrJson)
    .then(function (data) {
        return Object.entries(data["components"])
            .filter(([_, v]) => v.hasOwnProperty("status"))
            .map(([k, v]) => [k, v["status"]])
            .map(toTitleCase);
    })
    .then(entriesToList)
    .then(function (html) {
        insertHtmlBeforeEnd("actuator-health", html);
    })
    .catch(handleErrors);

fetch("/actuator/info")
    .then(throwOrJson)
    .then(entriesToNestedList)
    .then(function (html) {
        insertHtmlBeforeEnd("actuator-info", html);
    })
    .catch(handleErrors);

function insertHtmlBeforeEnd(elementId, html) {
    document
        .getElementById(elementId)
        .insertAdjacentHTML("beforeend", html);
}

function entriesToNestedList(data) {
    return entriesToList(Object.entries(data)
        .map(([k, v]) => [k, typeof v === "object" && Object.keys(v).length > 0 ? entriesToNestedList(v) : v])
        .map(toTitleCase)
    );
}

function entriesToHrefList(entries) {
    return entriesToList(entries, ([k, v]) => `<a href="${v}">${k}</a>`);
}

function entriesToList(entries, keyValueMapper = ([k, v]) => `${k}: ${v}`) {
    let html = ["<ul>"];
    entries
        .map(keyValueMapper)
        .map(a => `<li>${a}</li>`)
        .forEach(a => html.push(a));
    html.push("</ul>")
    return html.join("");
}

function toTitleCase([k, v], toLowerTail = false) {
    return [k.replace(/\w\S*/g, function (txt) {
        return txt.charAt(0).toUpperCase() + (toLowerTail ? txt.substring(1).toLowerCase() : txt.substring(1));
    }), v];
}

function throwOrJson(response) {
    if (!response.ok) {
        throw Error(response.statusText);
    }
    return response.json();
}

function handleErrors(error) {
    return console.log(error);
}
