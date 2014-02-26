A plugin for ubicity that goes & fetches Flickr photos, on demand. 
This plugin implements ReverseControllableMediumPlugin, which means that it works in exactly the reverse way from the TwitterPlugin:

it assumes to receive Command objects ( on a ServerSocket, default port 9876 ) from the ubicity "on demand" elasticsearch plugin, running in an unspecified locaiton. The Command is then examined upon search terms, and the plugin searches Flickr for exactly those terms, placing URLs of results in a dedicated elasticsearch /flickr index. 



[![Bitdeli Badge](https://d2weczhvl823v0.cloudfront.net/ubicity-principal/ubicity-flickrplugin/trend.png)](https://bitdeli.com/free "Bitdeli Badge")

