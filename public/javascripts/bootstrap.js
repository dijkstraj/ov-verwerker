var includes = [
	'//ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js',
	'//code.google.com/p/ov-verwerker/source/browse/public/javascripts/ov-verwerker.js'
];

function include(includes) {
	var inc = includes.shift();
	var jA = document.createElement('script');
	jA.setAttribute('type', 'text/javascript');
	jA.setAttribute('src', inc);
	if (includes.length > 0) {
		jA.onload = function() { include(includes); };
	}
	document.body.appendChild(jA);
}

include(includes);

var css = document.createElement('link');
css.setAttribute('media', 'screen');
css.setAttribute('rel', 'stylesheet');
css.setAttribute('type', 'text/css');
css.setAttribute('href', '//code.google.com/p/ov-verwerker/source/browse/public/stylesheets/ov-verwerker.css');
document.head.appendChild(css);
