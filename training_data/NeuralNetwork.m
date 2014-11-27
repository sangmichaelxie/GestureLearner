
function NeuralNetwork
    clear all; clear;
    
    %Using the larger test data for training increases performance
    O = load('O.txt');
    X = load('X.txt');
    Z = load('Z.txt');
    num_features = size(O ,2);
    %plotGestureData(O, 1);
    %plotGestureData(X, 2);
    %plotGestureData(Z, 3);
    
    training_instance_matrix = [O; X; Z;];
    temp_a = horzcat(ones(1, size(O, 1)), zeros(1, size(X, 1)), zeros(1, size(Z, 1)));
    temp_b = horzcat(zeros(1, size(O, 1)), ones(1, size(X, 1)), zeros(1, size(Z, 1)));
    temp_c = horzcat(zeros(1, size(O, 1)), zeros(1, size(X, 1)), ones(1, size(Z, 1)));
    training_label_vector = [temp_a; temp_b; temp_c];
    training_label_vector_orig = [zeros(size(O, 1), 1); ones(size(X, 1), 1); 2 * ones(size(Z, 1), 1);];
    
    
    %Smoothing with box filter seems to work better than gaussian filter
    training_instance_matrix = smoothts(training_instance_matrix, 'b', 25);
    %plotGestureData(training_instance_matrix(1:size(O, 1),:), 4);
    
    
    inputs = training_instance_matrix';
    targets = training_label_vector;
    
    size(inputs)
    size(targets)

    % Create a Pattern Recognition Network
    hiddenLayerSize = 11;
    net = patternnet(hiddenLayerSize);


    % Set up Division of Data for Training, Validation, Testing
    net.divideParam.trainRatio = 25 / 93;
    net.divideParam.valRatio = 0 / 93;
    net.divideParam.testRatio = 68 / 93;


    % Train the Network
    [net,tr] = train(net,inputs,targets);

    % Test the Network
    outputs = net(inputs)
    sum(vec2ind(targets) == vec2ind(outputs)) / size(targets, 2)
    %errors = gsubtract(targets,outputs)
    %performance = perform(net,targets,outputs)
    
    
    testX = inputs(:,tr.testInd);
    testT = targets(:,tr.testInd)

    testY = net(testX)
    testIndices = vec2ind(testY)
    vec2ind(testT)

    

    % View the Network
    %view(net)
    
    
    % Plots
    % Uncomment these lines to enable various plots.
    % figure, plotperform(tr)
    % figure, plottrainstate(tr)
    % figure, plotconfusion(targets,outputs)
    % figure, ploterrhist(errors)
    

end


function test(model)
    O_test = load('O.txt');
    X_test = load('X.txt');
    Z_test = load('Z.txt');
    
    testing_instance_matrix = [O_test; X_test; Z_test;];
    testing_label_vector = [zeros(size(O_test, 1), 1); ones(size(X_test, 1), 1); 2 * ones(size(Z_test, 1), 1);];
    
    model.predict(testing_instance_matrix) - testing_label_vector
    
    size(testing_label_vector)
end

function [X,Y,Z] = splitData(G)
    X = G(:, 1:100);
    Y = G(:, 101:200);
    Z = G(:, 201:300); 
end

function plotGestureData(G, figure_count)
    figure_num = (figure_count - 1) * 2 + 1;
    figure(figure_num);
    [X,Y,Z] = splitData(G);
    for i = 1:size(X,1)
        plot3(X(i,:),Y(i,:),Z(i,:));
        hold on;
    end
    title('All training examples');
    hold off;
    
    figure(figure_num + 1);
    plot3(X(1,:),Y(1,:),Z(1,:));
    title('One(first) training example');
    
end

function numCorrect = findNumCorrect(pred, actual)
    numCorrect = sum(pred == actual);
end

function [X_train, X_test, y_train, y_test] = getRandomSplitExamples(X, y, m)
    indices = datasample(1:size(X,1), m, 'Replace',false);
    X_train = zeros(m, size(X,2));
    X_test = zeros(size(X,1) - m, size(X,2));
    y_train = zeros(m, 1);
    y_test = zeros(size(y ,1) - m, 1);
    
    x_train_count = 1;
    x_test_count = 1;
    y_train_count = 1;
    y_test_count = 1;
    for i = 1:size(X,1)
        if any(i==indices)
            X_train(x_train_count, :) = X(i,:);
            y_train(y_train_count, :) = y(i,:);
            x_train_count = x_train_count + 1;
            y_train_count = y_train_count + 1;
        else
            X_test(x_test_count, :) = X(i, :);
            y_test(y_test_count, :) = y(i, :);
            x_test_count = x_test_count + 1;
            y_test_count = y_test_count + 1;
        end
        
    end
end