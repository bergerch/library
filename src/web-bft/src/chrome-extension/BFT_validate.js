console.log('Start: BFT verify');

var verify = false;
var replicaSet = [];
var hashes = [];
var computed_client_hash = 0x0;

// Intercept URL bar and cut out the hostname, e.g. if the url states my-app.com/editor then my-app.com is the hostname

chrome.tabs.query({'active': true, 'lastFocusedWindow': true}, function (tabs) {
  var url = tabs[0].url;
  url = url.replace('https://', '');
  url = url.replace('http://', '');
  var urlSplit = url.split('/');
  var appName = urlSplit [0];
  console.log('Searching replica set for  ', appName);

  var testName = 'google.com';

  var name = testName;

  // Make a DNS query to resolve the hostname to ALL records.
  // The result now is a list of IP addresses which correspond to the replica set.
  resolveName(name);

  // Meanwhile compute client code hash;
  computed_client_hash = computeClientCodeHash();

});

function resolveName(name) {
  if (name) {
    // Resolve hostname to IP
    var x = new XMLHttpRequest();
    x.open('GET', 'https://dns-api.org/A/' + name);
    x.onload = function() {
      console.log('DNS Query successful with , ', x.responseText);
      var result = JSON.parse(x.responseText);
      console.log('Parsed, ', result);

      for (var i = 0; i < result.length; i++) {
        replicaSet.push(result[i].value);
      }
      console.log('Replica set is ', replicaSet);

      // For every replica: obtain the hash of the client code
      requestHashesFromReplicas(replicaSet);
    };
    x.send();
  }
}

function requestHashesFromReplicas(replicaSet) {
  var x = [];
  for (var i = 0; i < replicaSet.length; i++) {
    x[i] = new XMLHttpRequest();
    x[i].open('GET', replicaSet[i] + '/verify_hash');
    x[i].onload = function() {
      console.log('Received hash of client code from replica', replicaSet[i], ' which is ',  x.responseText);
      var result = JSON.parse(x.responseText);
      console.log('Parsed hash, ', result);
      hashes[replicaSet[j]] = result;

      // Compute hash of client code and compare it with the obtained hash values of the other replicas.
      // Quorum for success is f+1

      validate(hashes);
    };
    x[i].send();
  }
}

function validate(hashes) {
  var n = replicaSet.length;
  var f = Math.floor((n-1) / 3 );
  var quorum = f + 1;

  if (hashes.length >= quorum) {
    var matching = 0;

    for (var i = 0; i < n; i++) {
      matching = hashes[i] === computed_client_hash ? matching++ : matching;
    }

    if (matching >= quorum)  {
      verify = true;
    }
  }
  notify(verify);
}

function modifyDOM() {
  console.log('Tab script:');
  var code = document.body.innerHTML;
  return code;
}

function computeClientCodeHash() {
  console.log('compute Hash');
  //We have permission to access the activeTab, so we can call chrome.tabs.executeScript:
  chrome.tabs.executeScript({
    code: '(' + modifyDOM + ')();' //argument here is a string but function.toString() returns function's code
  }, function f(code) {
      var hash = CryptoJS.enc.Hex.stringify(CryptoJS.SHA1(code));
      console.log(hash);
      return hash;
  });
}

function notify(verify) {
  if (verify) {
    console.log('Successfully verified with ', computed_client_hash);
    // TODO Print a Pop-Up on screen: Notify the user about the success  of bootstrapping the application in a BFT way
  }
}
