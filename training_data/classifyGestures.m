
function classifyGestures  
    clear all; clear;
    
    %Using the larger test data for training increases performance
    O = load('O.txt');
    X = load('X.txt');
    Z = load('Z.txt');
	V = load('V.txt');
      
    num_features = size(O ,2);
    plotGestureData(O, 1);
    plotGestureData(X, 2);
    plotGestureData(Z, 3);
	plotGestureData(V, 3);
	
	smoothO = smoothGestureData(O);
	smoothX = smoothGestureData(X);
	smoothZ = smoothGestureData(Z);
	smoothV = smoothGestureData(V);
    
    %training_instance_matrix = [O; X; Z;];
	training_instance_matrix = [smoothO; smoothX; smoothZ; smoothV;];
    training_label_vector = [zeros(size(O, 1), 1); ones(size(X, 1), 1); 2 * ones(size(Z, 1), 1); 3 * ones(size(Z, 1), 1);];
    
    %training_instance_matrix = zeroOutAndShift(training_instance_matrix);
    %zeroing out doesn't work
    
    %Smoothing with box filter seems to work better than gaussian filter
    training_instance_matrix = smoothts(training_instance_matrix, 'b', 25);
    %plotGestureData(training_instance_matrix(1:size(O, 1),:), 4);
    
    m = 60%round(size(training_instance_matrix, 1) * 7 / 10);
	
	min_endpoint = round(size(training_instance_matrix, 1) * 7 / 10);
	max_endpoint = round(size(training_instance_matrix, 1) * 7 / 10);
	
	trainAccuracy = zeros(1, max_endpoint - min_endpoint + 1);
	testAccuracy = zeros(1, max_endpoint - min_endpoint + 1);
    
	%for m = min_endpoint:max_endpoint
	    numCorrect = 0;
	    numCorrectTrain = 0;
	    %Resample
	    iterations = 1000;
	    for i = 1:iterations
	        [X_train, X_test, y_train, y_test] = getRandomSplitExamples(training_instance_matrix, training_label_vector, m);
	        %radial basis (gaussian) SVM (set -v 10 for 10-fold cross
	        %validation)
	        model = svmtrain(y_train, X_train, '-s 0 -t 2');
	        %Training error - it's always 100%
	        train_predictions = svmpredict(y_train, X_train, model);
	        numCorrectTrain = numCorrectTrain +  findNumCorrect(train_predictions, y_train);
	        %Testing error
	        test_predictions = svmpredict(y_test, X_test, model);
	        numCorrect = numCorrect +  findNumCorrect(test_predictions, y_test);
	    end
	    trainAccuracy(1, m - min_endpoint + 1) = numCorrectTrain / (iterations * m)
	    testAccuracy(1, m - min_endpoint + 1) = numCorrect / (iterations * (size(training_instance_matrix, 1) - m))   
	%end
	return;
	%%% Plot "Bias and Variance" %%%
	
	fig = figure;
	hold on;

	X_data = min_endpoint:max_endpoint;
	plot(X_data, 1 - trainAccuracy, 'b');
	plot(X_data, 1 - testAccuracy, 'r');

	title('SVM Bias and Variance');
	xlabel('Number of Training Examples');
	ylabel('Classification Error');
	legend('Training', 'Test');
	% for some reason I can't view the plot, so I save it
	print -dpdf fig-no-smoothing; % saved in fig.pdf
	saveas(fig, 'plot-no-smoothing.png')
	
end

function newX = zeroOutAndShift(X)
    newX = zeros(size(X, 1), size(X, 2));
    for i = 1:size(X,1)
        for t = 2:(size(X, 2) / 3)
            
            accelVec = [X(i, t); X(i, 100 + t); X(i, 200 + t);];
            accelVecPrev = [X(i, t - 1); X(i, 100 + t - 1); X(i, 200 + t - 1);];
            if norm(accelVec - accelVecPrev) < 0.5
                X(i, t) = 0; 
                X(i, t + 100) = 0; 
                X(i, t + 200) = 0;
            end
        end
        
        firstNonZeroIndex  = -1;
        X(i, 1) = 0;
        X(i, 101) = 0;
        X(i, 201) = 0;
        X(i, 100) = 0;
        X(i, 200) = 0;
        X(i, 300) = 0;
        for t = 2:(size(X, 2) / 3 - 1)
            accelVecForward = [X(i, t + 1); X(i, 100 + t + 1); X(i, 200 + t + 1);];
            accelVec = [X(i, t); X(i, 100 + t); X(i, 200 + t);];
            accelVecPrev = [X(i, t - 1); X(i, 100 + t - 1); X(i, 200 + t - 1);];
            diff = accelVecForward - accelVecPrev;
            if diff(1) == 0 || diff(2) == 0 || diff(3) == 0
                X(i, t) = 0; 
                X(i, t + 100) = 0; 
                X(i, t + 200) = 0;
            else 
                if firstNonZeroIndex == -1
                    firstNonZeroIndex = t;
                end
            end
        end
        
        
            
        %Shift examples
        newExample = zeros(1, size(X, 2));
        for t = 1:(size(X, 2) / 3)
            if firstNonZeroIndex > (size(X, 2) / 3)
                newExample(1,t) = 0;
                newExample(1,100 + t) = 0;
                newExample(1,200 + t) = 0;
                continue;
            elseif firstNonZeroIndex == -1 
                firstNonZeroIndex = 0;
            end
            newExample(1,t) = X(i,firstNonZeroIndex);
            newExample(1,100 + t) = X(i,100 + firstNonZeroIndex);
            newExample(1,200 + t) = X(i,200 + firstNonZeroIndex);
            firstNonZeroIndex = firstNonZeroIndex + 1;
        end
        newX(i, :) = newExample;
        firstNonZeroIndex
        newExample
    end

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

function GG = smoothGestureData(G)
	[GX, GY, GZ] = splitData(G);
	
	GX = smoothData(GX);
	GY = smoothData(GY);
	GZ = smoothData(GZ);
	
	GG = [GX GY GZ];
end

function output = smoothData(input)
	output = smoothts(input, 'b', 25);
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