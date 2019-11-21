filename = 'archive.txt';
delimiterIn = ' ';  
headerlinesIn = 0;
Archive = importdata(filename,delimiterIn,headerlinesIn);
plot(Archive(:,1),Archive(:,2),'ko','MarkerSize',4,'markerfacecolor','k');