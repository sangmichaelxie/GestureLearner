
function classifyGestures
    clear all; clear;
    O = load('O.txt');
    plotGestureData(O);
    
    
    
    
end

function [X,Y,Z] = splitData(G)
    X = G(:, 1:100);
    Y = G(:, 101:200);
    Z = G(:, 201:300); 
end

function plotGestureData(G)
    [X,Y,Z] = splitData(G);
    for i = 1:size(X,1)
        plot3(X(i,:),Y(i,:),Z(i,:));
        hold on;
    end
    title('All training examples');
    hold off;
    
    figure;
    plot3(X(1,:),Y(1,:),Z(1,:));
    title('One(first) training example');
    
end