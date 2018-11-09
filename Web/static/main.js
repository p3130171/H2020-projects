// main.js

$(document).ready(function(){
  $("#about-btn").click(function (e) {
    $("#home-fragment").hide();
    $("#about-fragment").show();
    $(".topnav").children().removeClass("active");
    $(this).addClass("active");
  });
  $("#home-btn").click(function (e) {
    $("#home-fragment").show();
    $("#about-fragment").hide();
    $(".topnav").children().removeClass("active");
    $(this).addClass("active");
  });
  $.get("static/about.md", function(data) {
      var converter = new showdown.Converter();
      var htmlDoc   = converter.makeHtml(data);
      $("#about-contents").html(htmlDoc);
  });
})

function populate_graph(datasource) {
  var config = {
    backgroundColor: "FFFFFF00",
    dataSource: datasource,
    forceLocked: true,
    directedEdges: false,
	collisionDetection: true, //NEW
	scaleExtent:[0.5, 2.4], 
	
    //nodeCaption: function(d) { return d.caption },
    nodeTypes: { "type": ["project", "contributor"] },
    nodeStyle: {

      "project": {
        "color": "#FFD23F",
        "borderColor": "darkred",
		"radius": 15,
		"borderWidth": 5,
		"captionColor": "#42AA3F",
		"captionBackground": "null",
		"captionSize": 12,
		"selected": {
			"borderColor": "white"
		},
		"highlighted": {
			"borderColor": "gray"
		},
		"nodeOverlap": 100
      },
      "contributor": {
		"radius": 10,
        "color": "FFD23F",
		"highlighted": {
			"borderColor": "gray",
			"borderWidth": 4
		},
		"borderWidth": 0,
		"nodeOverlap": 100
      }
    },
    edgeStyle: {
      "all": {
        "width": 2,
		"color": "#424242",
        "opacity": 0.9,
        "selected": {
            "opacity": 1
        },
        "highlighted": {
            "opacity": 1
        },
        "hidden": {
            "opacity": 0
        }
      }
    },
    nodeMouseOver: function(d) {
      var p = d.getProperties().data;
      var detailsElem = document.getElementById("details-container");
      //detailsElem.innerHTML = JSON.stringify(p, null, 2);
      while (detailsElem.firstChild) {
          detailsElem.removeChild(detailsElem.firstChild);
      }
      renderjson.set_show_to_level(1);
      renderjson.set_max_string_length(135);
      detailsElem.appendChild(renderjson(p));
      
      // var str = JSON.stringify(p, null, 2)
      // document.getElementById("details-container").innerHTML = str;
    },
    nodeClick: function(d) {
      var p = d.getProperties()
      if (p.type == "project") {
        window.location.href='/?rid='+d.id;
      }
    },
    nodeCaptionsOnByDefault: true,
    zoomControls: true,
	nodeRadius: 10, // Distance of the text from the node.
	nodeOverlap: 100,
	linkDistancefn: 200
  };
  alchemy = new Alchemy(config)
}

function getParameterByName(name, url) {
    if (!url) url = window.location.href;
    name = name.replace(/[\[\]]/g, "\\$&");
    var regex = new RegExp("[?&]" + name + "(=([^&#]*)|&|#|$)"),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';
    return decodeURIComponent(results[2].replace(/\+/g, " "));
}

var query = getParameterByName('query');
var rid = getParameterByName('rid');

if (query != null) {
  populate_graph('/search?query='+query)
} else if (rid != null) {
  populate_graph('/related/'+rid);
} else {
  populate_graph('/search?query='+'medical')
}
