import pandas as pd
import numpy as np

# Define the structure of the DataFrame as per the image provided
columns = ['CIF No', 'Product', 'CAMPAIGN_TYPE', 'Campaign Category', 'Score (ran 0 to 1)', 'Revenue per Product (range: 0 to 1)', 'Expected Conversion in Percentage', 'Actual Conversion in Percentage']

# Randomly generate 'CIF No' starting from a random 6 digit number
cif_start = np.random.randint(100000, 999999, size=100)

# Products as seen in the image
products = ['EPP', 'BT', 'CASA', 'CC']

# Campaign types as seen in the image
campaign_types = ['Acquisition', 'Deepening', 'Retention']

# Campaign categories based on the campaign types
campaign_category = {
    'Acquisition': 'Rule Based',
    'Deepening': 'Rule Based',
    'Retention': 'AI-ML'
}

# Score is a random float between 0 to 1
score = np.random.rand(100)

# Revenue per Product is a random float between 0 to 1
revenue_per_product = np.random.rand(100)

# Expected and Actual Conversion in Percentage logic based on the footnote
expected_conversion = {
    'Acquisition': np.random.uniform(3, 5, size=100),
    'Deepening': np.random.uniform(7, 8, size=100),
    'Retention': np.random.uniform(5, 6, size=100)
}

# Generate 100 rows of dummy data
rows = []
for i in range(100):
    product = np.random.choice(products)
    campaign_type = np.random.choice(campaign_types)
    rows.append([
        cif_start[i],
        product,
        campaign_type,
        campaign_category.get(campaign_type, 'Rule Based'),
        score[i],
        revenue_per_product[i],
        np.random.choice(expected_conversion[campaign_type]),
        np.random.uniform(0.5, 1.5) + np.random.choice(expected_conversion[campaign_type])  # Adding a random float to the expected conversion for actual conversion
    ])

# Create the DataFrame
dummy_data = pd.DataFrame(rows, columns=columns)

dummy_data.head()
