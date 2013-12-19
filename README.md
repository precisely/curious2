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

1. Uncheck all Less Components except **Grid system, Basic utilities, Responsive utilities, Component animations**,
2. Set @container-lg width to 1000px,
3. Set @grid-gutter-width to 20px.

Out of this customization, after downloading resources, remove **!important** in **.hide** class CSS from bootstrap.css & bootstrap.min.css    
This is required because, we're using **hide** class in most portions & jQuery's **show()** method can't display element due to important mark in css.