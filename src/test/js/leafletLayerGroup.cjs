function layerGroup() {
    const layers = new Set();
    let map;

    return {
        addLayer(layer) {
            layers.add(layer);
            map?.layers.add(layer);
            return this;
        },
        clearLayers() {
            for (const layer of layers) map?.layers.delete(layer);
            layers.clear();
            return this;
        },
        getLayers() {
            return [...layers];
        },
        addTo(target) {
            map = target;
            map.layers.add(this);
            for (const layer of layers) map.layers.add(layer);
            return this;
        },
        remove() {
            if (map) {
                for (const layer of layers) map.layers.delete(layer);
                map.layers.delete(this);
                map = undefined;
            }
            return this;
        }
    };
}

module.exports = layerGroup;
