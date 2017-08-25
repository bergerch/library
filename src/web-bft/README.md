# WebBft

This project was generated with [angular-cli](https://github.com/angular/angular-cli) version 1.0.0-beta.24.

## Development server
Run `ng serve` for a dev server. Navigate to `http://localhost:4200/`. The app will automatically reload if you change any of the source files.
Use Run `ng serve --host 0.0.0.0 --port 1234` for a dev web server with specific address and port

## Code scaffolding

Run `ng generate component component-name` to generate a new component. You can also use `ng generate directive/pipe/service/class/module`.

## Build

Run `ng build` to build the project. The build artifacts will be stored in the `dist/` directory. Use the `-prod` flag for a production build. 

## Configuring the ports

In TOMConfiguration.ts, the replica set can be configured like this (example):

  /** Hosts Configurations */

  hosts: Host[] = [
    {server_id: 0, address: '132.231.1.138', port: 11005},
    {server_id: 1, address: '132.231.1.138', port: 11015},
    {server_id: 2, address: '132.231.1.138', port: 11025},
    {server_id: 3, address: '132.231.1.138', port: 11035}
  ];

Important (!): the port number should always end with 5, ergo portnumber % 5 == 0

in BFT-SMaRts hosts.config, the ports should look like this: (port numbers ending with a 0)

#server id, address and port (the ids from 0 to n-1 are the service replicas) 
0 132.231.1.138 11000
1 132.231.1.138 11010
2 132.231.1.138 11020
3 132.231.1.138 11030

Every replica should have a port number that is at least +10 to the last one. 
The reason for this is, that multiple ports are used by a replica.

## Running the Demo

As a Demo, a collaborative editor was implemented. The following steps are necessary to test

1) For n replica: in BFT-SMaRt demo folder, run CollabEditServer and suply  the replica ID as program argument eg 0,1,2,3

2) in src/web-bft run command 'ng serve' in the shell. This will start a web dev server delivering the web client

3) In the browser: Open localhost:4200/editor and you are ready to use the application

## Deploying to Github Pages

Run `ng github-pages:deploy` to deploy to Github Pages.

## Further help

To get more help on the `angular-cli` use `ng help` or go check out the [Angular-CLI README](https://github.com/angular/angular-cli/blob/master/README.md).
