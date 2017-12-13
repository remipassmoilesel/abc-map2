import Vue from 'vue';
import Component from 'vue-class-component';
import {Clients} from '../../lib/clients/Clients';
import {AbstractMapLayer} from "../../../api/entities/layers/AbstractMapLayer";
import {IUxComponent} from "../IUxComponent";
import './style.scss';

@Component({
    template: require('./template.html'),
})
export class AddLayerComponent extends Vue implements IUxComponent {

    public name = "Add a Tile layer";
    public description = "Add a tile layer in order to display a world map";

    public clients: Clients;
    public layers: AbstractMapLayer[] = [];

    public beforeMount() {
        this.clients.map.getDefaultWmsLayers()
            .then((layers) => {
                this.layers = layers;
            })
            .catch((e) => {
                console.log(e);
            });
    }

    public handleSelection(index: number) {
        this.clients.project.addLayer(this.layers[index]);
    }

}
