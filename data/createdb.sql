create text table vehicles(vin varchar(32) primary key, year integer, make varchar(32), model varchar(32));
set table vehicles source "vins.csv";