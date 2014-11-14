function KNearestNeighbors  
    clear all; clear;
    
    %Using the larger test data for training increases performance
    O = load('O.txt');
    X = load('X.txt');
    Z = load('Z.txt');
    
    
    training_instance_matrix = [O; X; Z;];
    training_label_vector = [zeros(size(O, 1), 1); ones(size(X, 1), 1); 2 * ones(size(Z, 1), 1);];
    training_instance_matrix = smoothts(training_instance_matrix, 'b', 25);
    
    m = round(size(training_instance_matrix, 1) * 7 / 10);    
    
    numCorrect = 0;
    numCorrectTrain = 0;
    avgKFoldLoss = 0;
    %Resample
    iterations = 100;
    for i = 1:iterations
        [X_train, X_test, y_train, y_test] = getRandomSplitExamples(training_instance_matrix, training_label_vector, m);
        %K-Nearest-Neighbors
        
        %[idx, C] = kmeans(X_train, 3);
        
        mdl = ClassificationKNN.fit(X_train, y_train,'NumNeighbors',4);
        cvmdl = crossval(mdl);
        kloss = kfoldLoss(cvmdl);
        avgKFoldLoss = avgKFoldLoss + kloss;
        train_predictions = predict(mdl, X_train);
        test_predictions = predict(mdl, X_test);   
        
        %Training error
        numCorrectTrain = numCorrectTrain +  findNumCorrect(train_predictions, y_train);
        
        %Testing error
        %test_predictions = nearestCentroidClassifier(C, X_test);
        numCorrect = numCorrect +  findNumCorrect(test_predictions, y_test);
    end
    avgKFoldLoss = avgKFoldLoss / iterations
    trainAccuracy = numCorrectTrain / (iterations * m )
    testAccuracy = numCorrect / (iterations * (size(training_instance_matrix, 1) - m))    
    
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