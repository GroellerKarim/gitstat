<!DOCTYPE html>
<html>
<head>
    <title>Git Repository Statistics</title>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/moment"></script>
    <script src="https://cdn.jsdelivr.net/npm/chartjs-adapter-moment"></script>
</head>
<body>
    <div style="width: 1200px; margin: 20px auto;">
        <canvas id="commitsChart"></canvas>
    </div>
    <div style="width: 1200px; margin: 20px auto;">
        <canvas id="changesChart"></canvas>
    </div>

    <script>
        const commonOptions = {
            responsive: true,
            interaction: {
                intersect: false,
                mode: 'index'
            },
            scales: {
                x: {
                    type: 'time',
                    time: {
                        unit: 'month',
                        displayFormats: {
                            month: 'MMM YYYY'
                        }
                    },
                    grid: {
                        display: true,
                        drawBorder: false,
                        borderDash: [8, 4]
                    },
                    ticks: {
                        maxRotation: 45,
                        minRotation: 45
                    }
                },
                y: {
                    beginAtZero: true,
                    type: 'linear',
                    grid: {
                        display: true,
                        drawBorder: false,
                        borderDash: [8, 4]
                    }
                }
            },
            elements: {
                line: {
                    tension: 0.4,
                    borderWidth: 2
                },
                point: {
                    radius: 0,
                    hitRadius: 10,
                    hoverRadius: 4
                }
            },
            plugins: {
                tooltip: {
                    callbacks: {
                        title: function(context) {
                            return moment(context[0].label).format('YYYY-MM-DD');
                        }
                    }
                }
            }
        };

        const commitsData = {
            labels: ${dates},
            datasets: [{
                label: 'Commits',
                data: ${commits},
                borderColor: 'rgb(47, 129, 247)',
                backgroundColor: 'rgba(47, 129, 247, 0.1)',
                fill: true,
                tension: 0.4
            }]
        };

        const changesData = {
            labels: ${dates},
            datasets: [{
                label: 'Additions',
                data: ${additions},
                borderColor: 'rgb(46, 160, 67)',
                backgroundColor: 'rgba(46, 160, 67, 0.1)',
                fill: true,
                tension: 0.4
            },
            {
                label: 'Deletions',
                data: ${deletions},
                borderColor: 'rgb(248, 81, 73)',
                backgroundColor: 'rgba(248, 81, 73, 0.1)',
                fill: true,
                tension: 0.4
            }]
        };

        new Chart(
            document.getElementById('commitsChart'),
            {
                type: 'line',
                data: commitsData,
                options: {...commonOptions, 
                    plugins: {
                        ...commonOptions.plugins,
                        title: {
                            display: true,
                            text: 'Commits Over Time'
                        }
                    }
                }
            }
        );

        new Chart(
            document.getElementById('changesChart'),
            {
                type: 'line',
                data: changesData,
                options: {...commonOptions,
                    plugins: {
                        ...commonOptions.plugins,
                        title: {
                            display: true,
                            text: 'Code Changes Over Time'
                        }
                    }
                }
            }
        );
    </script>
</body>
</html> 