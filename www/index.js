var page = new tabris.Page({
    title: "Test Tabris",
    topLevel: true
});

var fullscreen = true;

var cordovaConfigOnScreen = {
    "xPosition": 70,
    "yPosition": 180,
    "width": 225,
    "height": 125
};

var cordovaConfigFullscreen = {
    "xPosition": 0,
    "yPosition": 0,
    "width": Math.round(screen.width),
    "height": Math.round(screen.height),
    fullscreen: true
};

new tabris.ToggleButton({
    layoutData: {centerY: 0, centerX: 0},
    text: "selected",
    selection: fullscreen
}).on("change:selection", function (button, selection) {
    this.set("text", selection ? "Fullscreen" : "On Screen");
    fullscreen = selection;
}).appendTo(page);


new tabris.Button({
    layoutData: {left: 0, right: 0, bottom: 0},
    background: "rgb(0, 113, 188)",
    textColor: "#fff",
    text: "Play"
}).appendTo(page).on("select", function (button, selection) {

    var options = {
        "content": [{
            "url": "http://www.w3schools.com/html/mov_bbb.mp4",
            "autoplay": true
        }]
    };

    options.cordovaConfig = fullscreen ? cordovaConfigFullscreen : cordovaConfigOnScreen;

    // Launch the player
    veeplay.playFromObject(options, function (res) {
        console.log("Success", res);
    }, function (err) {
        console.log("Error", err);
    });
});

page.open();