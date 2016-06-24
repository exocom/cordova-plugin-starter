var page = new tabris.Page({
    title: "Test Tabris",
    topLevel: true
});

var pressed = 0;

new tabris.Button({
    layoutData: {left: 0, right: 0, bottom: 0},
    background: "rgb(0, 113, 188)",
    textColor: "#fff",
    text: "Button"
}).appendTo(page).on("select", function (button, selection) {
    pressed++;
    console.log(pressed);
});

page.open();