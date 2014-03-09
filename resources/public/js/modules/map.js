define(['jQuery', 'Leaflet'], function($, L){
  var map;
  var position_mark;
  var zoomLevel = 16;

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
        color: '#E82E2E',
        fillColor: '#EF6767',
        fillOpacity: 0.5
      }).addTo(map).bindPopup('<i class=\"icon-user\"></i> You.');

      if (c) c(position);
    });
  };

  return {

    initialize: function(){
      map = L.map("map");
      L.tileLayer('https://{s}.tiles.mapbox.com/v3/sunng.hfhn9hcn/{z}/{x}/{y}.png', {
        attribution: '<a href="http://www.mapbox.com/about/maps/" target="_blank">Terms &amp; Feedback</a>'
      }).addTo(map);
      /*L.tileLayer('http://{s}.tile.osm.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
      }).addTo(map);*/
      //new CurrentPositionControl().addTo(map);
    },

    getMap: function(){
      return map;
    },

    viewCurrent: function(cb){
      setCurrentPosition(cb);
    },

    trackCurrent: function(c){
      navigator.geolocation.watchPosition(c);
    },

    addControl: function(html, cb){
      var ControlClass = L.Control.extend({
        options: {
          position: 'topright'
        },

        onAdd: function (map) {
          var container = $(html);
          container.click(cb);

          return container[0];
        }
      });
      new ControlClass().addTo(map);
    },

    newMarkerWithEvent: function(coords, clickcb){
      return L.circle(coords, 10, {
        color: 'red',
        fillColor: 'red',
        fillOpacity: 1,
        stroke: false
      }).on("click", clickcb);
    },

    newMarkerGroup: function(markers){
      return L.layerGroup(markers);
    },

    getCurrentPosition: function(){
      if (position_mark) {
        return position_mark.getLatLng();
      } else {
        throw "Unknown position";
      }
    },

    toggleCurrentositionIndicator: function(){
      if (map.hasLayer(position_mark)) {
        map.removeLayer(position_mark);
      } else {
        map.addLayer(position_mark);
      }
    }
  };
});
