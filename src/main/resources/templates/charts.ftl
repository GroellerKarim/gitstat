<!DOCTYPE html>
<html>
<head>
    <title>Git Repository Statistics</title>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/moment"></script>
    <script src="https://cdn.jsdelivr.net/npm/chartjs-adapter-moment"></script>
</head>
<body>
    <div style="width: 800px; margin: 20px auto;">
        <canvas id="commitsChart"></canvas>
    </div>
    <div style="width: 800px; margin: 20px auto;">
        <canvas id="changesChart"></canvas>
    </div>

    <script>
        const commitsData = {
            labels: ${dates},
            datasets: [{
                label: 'Commits',
                data: ${commits},
                borderColor: 'rgb(75, 192, 192)',
                tension: 0.1
            }]
        };

        const changesData = {
            labels: ${dates},
            datasets: [{
                label: 'Additions',
                data: ${additions},
                borderColor: 'rgb(75, 192, 75)',
                tension: 0.1
            },
            {
                label: 'Deletions',
                data: ${deletions},
                borderColor: 'rgb(192, 75, 75)',
                tension: 0.1
            }]
        };

        const config = {
            type: 'line',
            options: {
                responsive: true,
                scales: {
                    x: {
                        type: 'time',
                        time: {
                            unit: 'day',
                            displayFormats: {
                                day: 'YYYY-MM-DD'
                            }
                        }
                    }
                }
            }
        };

        new Chart(
            document.getElementById('commitsChart'),
            {...config, data: commitsData}
        );

        new Chart(
            document.getElementById('changesChart'),
            {...config, data: changesData}
        );
    </script>
</body>
</html> 