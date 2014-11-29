
function classifyGestures  
    clear all; clear;
    
    %Using the larger test data for training increases performance
    O = load('O.txt');
    X = load('X.txt');
    Z = load('Z.txt');
	V = load('V.txt');
    W = load('W.txt');
      
    %num_features = size(O ,2);
    %plotGestureData(O, 1);
    %plotGestureData(X, 2);
    %plotGestureData(Z, 3);
	%plotGestureData(V, 3);
	
	
	
	
	
	%O = truncateGestureData(O);
	%X = truncateGestureData(X);
	%Z = truncateGestureData(Z);
	%V = truncateGestureData(V);
	%W = truncateGestureData(W);
	
	
	
	smoothO = smoothGestureData(O);
	smoothX = smoothGestureData(X);
	smoothZ = smoothGestureData(Z);
	smoothV = smoothGestureData(V);
    smoothW = smoothGestureData(W);
	
	
	%smoothO = O;
	%smoothX = X;
	%smoothZ = Z;
	%smoothV = V;
	%smoothW = W;
	
	
	
	%smoothO = truncateGestureData(smoothO);
	%smoothX = truncateGestureData(smoothX);
	%smoothZ = truncateGestureData(smoothZ);
	%smoothV = truncateGestureData(smoothV);
	%smoothW = truncateGestureData(smoothW);

	%poll = 1;
	%smoothO = smoothO(:, 1:poll:300);
	%smoothX = smoothX(:, 1:poll:300);
	%smoothZ = smoothZ(:, 1:poll:300);
	%smoothV = smoothV(:, 1:poll:300);
	%smoothW = smoothW(:, 1:poll:300);
	

	size(O)
	size(smoothO)
    
	training_instance_matrix = [smoothO; smoothX; smoothZ; smoothV;smoothW;];
	
    training_label_vector = [zeros(size(O, 1), 1); ones(size(X, 1), 1); 2 * ones(size(Z, 1), 1); 3 * ones(size(V, 1), 1); 4 * ones(size(W,1),1)];
    numClasses = 5;
    
    %training_instance_matrix = zeroOutAndShift(training_instance_matrix);
    %zeroing out doesn't work
    
    %plotGestureData(training_instance_matrix(1:size(O, 1),:), 4);
    	
	min_endpoint = 1;
	max_endpoint = 30;
	
	trainAccuracy = zeros(1, max_endpoint - min_endpoint + 1);
	testAccuracy = zeros(1, max_endpoint - min_endpoint + 1);
    
    %m is examples from each class
	for m = min_endpoint:max_endpoint
	    numCorrect = 0;
	    numCorrectTrain = 0;
	    %Resample
	    iterations = 1000;
	    for i = 1:iterations
            
	        [X_train, X_test, y_train, y_test] = getRandomSplitExamples(training_instance_matrix, training_label_vector, m, numClasses);
	        
			%Normalize
			%X_train = X_train - mean(X_train(:));
			%X_test = X_test - mean(X_test(:));
			
			%X_train = X_train / std(X_train(:));
			%X_test = X_test / std(X_test(:));
			
			
			%radial basis (gaussian) SVM (set -v 10 for 10-fold cross
	        %validation)
	        model = svmtrain(y_train, X_train, '-s 0 -t 0'); %try linear kernel -t 0 or gaussian -t 2
	        %Training error - it's always 100%
	        train_predictions = svmpredict(y_train, X_train, model);
	        numCorrectTrain = numCorrectTrain +  findNumCorrect(train_predictions, y_train);
	        %Testing error
	        test_predictions = svmpredict(y_test, X_test, model);
	        numCorrect = numCorrect +  findNumCorrect(test_predictions, y_test);
	    end
	    trainAccuracy(1, m - min_endpoint + 1) = numCorrectTrain / (iterations * numClasses * m);
	    testAccuracy(1, m - min_endpoint + 1) = numCorrect / (iterations * (size(training_instance_matrix, 1) - numClasses * m));  
        
    end

	trainAccuracy
    testAccuracy
	%%% Plot "Bias and Variance" %%%
	
	fig = figure;
	hold on;

	X_data = min_endpoint:max_endpoint;
	plot(X_data, 1 - trainAccuracy, 'b');
	plot(X_data, 1 - testAccuracy, 'r');

	title('SVM Bias and Variance');
	xlabel('Number of Training Examples per Class');
	ylabel('Classification Error');
	legend('Training', 'Test');
	% for some reason I can't view the plot, so I save it
	print -dpdf fig; % saved in fig.pdf
	saveas(fig, 'plot-box-filter.png')
	
end

function numCorrect = findNumCorrect(pred, actual)
    %numCorrect = sum(pred == actual);
    numCorrect = 0 ;
    for i = 1:size(pred,1)
       if pred(i) == actual(i)
           numCorrect = numCorrect + 1;
       end
    end
end

function [X_train, X_test, y_train, y_test] = getRandomSplitExamples(X, y, m, numClasses)
    X_train = zeros(m * numClasses, size(X,2));
    X_test = zeros(size(X,1) - m * numClasses, size(X,2));
    y_train = zeros(m * numClasses, 1);
    y_test = zeros(size(y ,1) - m * numClasses, 1);
    
    x_train_count = 1;
    x_test_count = 1;
    y_train_count = 1;
    y_test_count = 1;
    
    currClass = 0;
    lastClassIndex = 1; %one past the index of the last class
    for i = 1:size(y,1)
        
        
        if i == size(y,1) || y(i,1) ~= currClass
            if i == size(y,1)
                currClassIndex = i;
            else
                currClassIndex = i-1;
            end
            
            indices = datasample(lastClassIndex:currClassIndex, m, 'Replace',false);
            
            for j = lastClassIndex:currClassIndex
                if any(j==indices)
                    X_train(x_train_count, :) = X(j,:);
                    y_train(y_train_count, :) = y(j,:);
                    x_train_count = x_train_count + 1;
                    y_train_count = y_train_count + 1;
                else
                    X_test(x_test_count, :) = X(j, :);
                    y_test(y_test_count, :) = y(j, :);
                    x_test_count = x_test_count + 1;
                    y_test_count = y_test_count + 1;
                end
            end
            
            lastClassIndex = i;
            currClass = y(i,1);
        end
        
        
        
    end
    
    shufflerMatrix = [X_test y_test];
    shufflerMatrix = shufflerMatrix(randperm(size(shufflerMatrix,1)),:);
    
    X_test = shufflerMatrix(:,1:end-1);
    y_test = shufflerMatrix(:,end);
end

function GG = truncateGestureData(G)
	
	GG = G;
	
	
	for k = 1:size(GG, 1)
		
		GG(k, :) = truncateGestureExample(GG(k, :));
		
	end
	
	%GG(1, :)
	
	%pause(10000000);
	
end

function GG = truncateGestureExample(G)
	
	[X, Y, Z] = splitData(G);
	
	% Truncate a fixed amount from both ends to remove noise.
	% If this is not done, then truncating a variable amount will not work 
	% effectively because for many training examples there is a sharp incline
	% between times 1 and 5; this incline appears to be noise.
	%
	% Truncating a fixed small amount doesn't appear to have any effect on
	% accuracy.
	
	noiseDelta = 5;
	
	X = X(1, (noiseDelta + 1):100);
	%GX(:, (100 - noiseDelta):100) = 0;
	
	Y = Y(1, (noiseDelta + 1):100);
	%GY(:, (100 - noiseDelta):100) = 0;
	
	Z = Z(1, (noiseDelta + 1):100);
	%GZ(:, (100 - noiseDelta):100) = 0;
	
	
	
	GX = X;
	GY = Y;
	GZ = Z;
	
	
	% Now time to truncate variable amount from both ends.
	%
	% Truncating variable amount won't be effective on gestures with change in
	% acceleration because the whole thing will be truncated! We will add some 
	% threshold on how much you can truncate.
		
	noiseNormThreshold = 1;
	
	
	% Truncate from left side
	
	bound = size(X, 2);
	
	
	
	
	for k = 1:bound
		
		diffNorm = sqrt((X(k) - X(1))^2 + (Y(k) - Y(1))^2 + (Z(k) - Z(1))^2);
		
		if (diffNorm < noiseNormThreshold)
			GX(1) = [];
			GY(1) = [];
			GZ(1) = [];
		else
			break;
		end
		
	end
	
	
	% Truncate from right side
	
	for k = bound:-1:1
		
		diffNorm = sqrt((X(k) - X(bound))^2 + (Y(k) - Y(bound))^2 + (Z(k) - Z(bound))^2);
		
		if (diffNorm < noiseNormThreshold)
			rightist = size(GX, 2);
			GX(rightist) = [];
			GY(rightist) = [];
			GZ(rightist) = [];
		else
			break;
		end
		
	end
	
	
	% Linear interpolation to grow back to size 300!
	
	%
	% Try different interpolations!
	%
	
	domain = 1:100;
	
	domain = (domain - 1) .* ((size(GX, 2) - 1) / (length(domain) - 1)) + 1;
	
	
	
	NGX = interp1(1:size(GX, 2), GX, domain);
	NGY = interp1(1:size(GY, 2), GY, domain);
	NGZ = interp1(1:size(GZ, 2), GZ, domain);
	
	

	%NGX
	
	%pause(10000000);

	
	
	%x = 100:-1:1
	
	%size(GX)
	
	%pause(100000000);
	
	
	GG = [NGX NGY NGZ];
end

function GG = smoothGestureData(G)
	[GX, GY, GZ] = splitData(G);
	
	GX = smoothData(GX);
	GY = smoothData(GY);
	GZ = smoothData(GZ);
	
	GG = [GX GY GZ];
end

function output = smoothData(input)
	%output = smoothts(input, 'b', 25);
	
	
	
	%Moving average filter
	%windowSize = 5;

	%b = (1/windowSize)*ones(1,windowSize)
	%a = 1;
	%output = filter(b,a,input);
	
	%Low pass filter
	a = 0.3;
	output = filter(a, [1 a-1], input)
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