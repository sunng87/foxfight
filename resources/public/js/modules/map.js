define(['jQuery', 'Leaflet'], function($, L){
  var map;
  var position_mark;
  var zoomLevel = 16;
  var clientId = null;

  var CurrentPositionControl = L.Control.extend({
    options: {
      position: 'topright'
    },

    onAdd: function (map) {
      var container = L.DomUtil.create('button', 'btn btn-mini');
      container.innerHTML = '<i class=\"icon-circle\"></i>'
      $(container).click(function(){
        setCurrentPosition();
      })
      
      return container;
    }
  });
  var ScanCircleControl = L.Control.extend({
    options: {
      position: 'topright'
    },

    onAdd: function (map) {
      var container = L.DomUtil.create('button', 'btn btn-danger btn-mini');
      container.innerHTML = '<i class=\"icon-eye-open\"></i>'
      $(container).click(function(){
        findNearPoints();
        var self = this;
        $(self).attr("disabled", "disabled");
        setTimeout(function(){
          $(self).removeAttr("disabled");
        }, 15000);
      });
      
      return container;
    }
  });

  var setCurrentPosition = function(c){
    navigator.geolocation.getCurrentPosition(function(position) {
      var coords = [position.coords.latitude, position.coords.longitude];
      map.setView(coords, zoomLevel);
      var accuracy = position.coords.accuracy;
      
      if (position_mark) {
        map.removeLayer(position_mark);
        position_mark = null;
      }
      
      position_mark = L.circle(coords, accuracy, {
        color: 'blue',
        fillColor: 'blue',
        fillOpacity: 0.5
      }).addTo(map).bindPopup('<i class=\"icon-user\"></i> You.');

      if (c) c(position);
    });
  };

  var findNearPoints = function() {
    setCurrentPosition(function(){
      $.ajax("/location/nearby", {
        data: {lat: position_mark.getLatLng().lat, 
               lon: position_mark.getLatLng().lng},
        type: "GET",
        success: function(r){
          var markers = [];
          r.forEach(function(i){

            if (i["client-id"] === clientId) {
              return ;
            } else {
              var coords = [i.loc.coordinates[1], i.loc.coordinates[0]];
              var m = L.circle(coords, 5, {
                color: 'red',
                fillColor: 'red',
                fillOpacity: 0.5
              }).bindPopup(
                "<i class=\"icon-user\"></i> " 
                  + i["client-id"].substring(0, 5)
                  + "***");
              markers.push(m);
            }
          });

          var lg = new L.LayerGroup(markers);
          lg.addTo(map);
          setTimeout(function(){
            map.removeLayer(lg);            
          }, 15000);
        }
      });
    });
  };

  return {
    
    initialize: function(){
      map = L.map("map");
      L.tileLayer('http://{s}.tile.osm.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
      }).addTo(map);
      new CurrentPositionControl().addTo(map);
      new ScanCircleControl().addTo(map);
    },

    getMap: function(){
      return map;
    },

    viewCurrent: function(){
      setCurrentPosition();
    },

    trackCurrent: function(c){
      navigator.geolocation.watchPosition(c);
    },
    
    setClientId: function(cid) {
      clientId = cid;
    }
  };
});

