Curious
=======

## Test Features

Following feature needs to tested after development:

### Common

1. User profile update page,
2. No javascript error on graph or track page.

### Track Page

1. Adding new entry,
2. In place tag editing,
3. Dragging & drop tag on add entry text field,
4. Adding ghost entry, activating ghost (regular & continuous both) entry.

### Graph Page

1. Drawing graph using tag & tagGroup,
2. Operation on graph using accordion

### Graph & Track Page common

1. Searching tag,
2. Creating Tag to TagGroup,
3. Creating TagGroup to TagGroup,
4. Add/remove pin from Tag & TagGroup,
5. Deleting TagGroup.

### Discussion Page

1. In place editing discussion topic,
2. Displaying graph after sharing,
3. Comment feature on discuss page.

## Bootstrap Customization

Following are some bootstrap customization which needs to be made before downloading new bootstrap resources:

Bootstrap customization ID: 04409b33eb78e4f6e3a6 (Also present in bootstrap.min.css)

This customization ID sets: 
1. Uncheck all Less Components except **Grid system, form, typography, Basic utilities, Responsive utilities**,
2. @container-lg width to 1000px,
3. @grid-gutter-width to 20px.

After downloading the customized files, remove **!important** in **.hide** class CSS from bootstrap.min.css    
This is required because, we're using **hide** class in most portions & jQuery's **show()** method can't display element due to important mark in css.

## New development machine setup

Install Vagrant from [here](https://www.vagrantup.com/downloads.html), and Docker From [here](https://docs.docker.com/installation/) on your development machine.    
Inside your project directory there is a script file `curious-env`, execute this file.    

### `curious-env` file performs following tasks:
 * Installs gvm in your local system if not already installed
 * Installs grails-2.4.3 in your local system if not already installed and copies it's installation directory into your project directory as `current-grails`.
 * Creates the Docker container using Vagrant and Docker file present in the project directory.
 * SSH into the created docker container.

### After Logging into the container first time:
 * Default user for the container is  **root**.
 * **After first login you need to set up MySql user name, password and create the database to be used for development**.

### TODOs after Logging into the container every time:
 * By default your project directory will be automatically synchronized with the container inside `/vagrant` directory.
 * `cd` into the `/vagrant` directory and run following command to run your project:
   `grails -reloading run-app`
 * Once server has started you can browse through `localhost/home/index`.(On browser, server will be running on port 80 NOT 8080, you can change this mapping in vagrant file)

Each time you need to start your machine Run following command inside your project directory:    
`sudo vagrant up --provider=docker --debug`

After successful execution of the above command you just have to ssh into the container:    
`sudo vagrant ssh`
